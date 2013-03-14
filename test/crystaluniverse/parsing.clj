(ns crystaluniverse.parsing
  (:use adi.utils)
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]))

(defn parse-long [v]
  (Long/parseLong (or v "0")))

(defn parse-double [v]
  (Double/parseDouble (or v "0")))

(defn parse-longs [arr]
  (->> arr (map parse-long) set))

(defn parse-bigdec [v]
  (bigdec (Double/parseDouble (or v "0"))))

(defn parse-is-same [st t]
  (if (= st t) true false))

(defn parse-instance [v]
  (tc/to-date (.replace v " " "T")))

(declare parse-categories
         categories-add-enabled categories-add-children)

(defn parse-categories [m]
  ;;(println (get m "position") m)
  (-> {:+ {:magento {:category {:id (parse-long (get m "category_id"))}}}
       :name (get m "name")
       :slug (clean-name (get m "name"))
       :position (parse-long (get m "position"))}
      (categories-add-enabled m)
      (categories-add-children m)))

(defn parse-all-categories [m]
  {:category (parse-categories m)})

(defn categories-add-enabled [output m]
  (let [enabled (get m "is_active")
        res  (cond (= "1" enabled) true
                   (= "0" enabled) false)]
    (if res
      (assoc output :enabled res)
        output)))

(defn categories-add-children [output m]
  (let [chs (get m "children")
        res (map #(parse-categories %) chs)]
    (assoc output :children (set res))))


(declare parse-products
         parse-single-product parse-grouped-product parse-single-variant
         parse-enabled parse-product-image parse-product-images
         parse-product-unit)

(defn parse-products [all]
  (let [grouped       (->> all
                           (filter (fn [[k v]] (= "grouped" (get v "type"))))
                          (into {}))
        ;;_             (println (first grouped))
        ids-grouped   (set (keys grouped))
        get-child-ids (fn [m] (map #(get % "product_id") (m "children")))
        ids-children  (->> grouped
                           (mapcat (fn [[k v]] (get-child-ids v)))
                           set)
        ids-single    (-> (set (keys all))
                          (clojure.set/difference ids-grouped ids-children))]
    (concat
     (map (fn [k] (parse-single-product (all k))) ids-single)
     (map (fn [k] (parse-grouped-product (all k) all)) ids-grouped))))

(defn parse-single-product [m]
  (let [md (first (m "details"))]
    {:magento {:product {:id (parse-long (m "product_id"))
                         :categories (parse-longs (m "category_ids"))
                         :active true
                         :type :single}}
     :history {:added (parse-instance (md "created_at"))}
     :product {:sku    (m "sku")
               :name   (m "name")
               :slug   (md "url_key")
               :unit   (parse-product-unit (md "sell_by"))
               :weight (parse-bigdec (md "weight"))
               :desc   {:long   (md "description")
                        :short  (md "short_description")}
               :enabled true
               :price  (parse-bigdec (md "price"))
               :images (parse-product-images (m "media"))}}))

(defn parse-single-variant [m pos]
  (let [md (first (m "details"))]
    {:+ {:magento {:product {:id (parse-long (m "product_id"))
                             :categories (parse-longs (m "category_ids"))
                             :active false
                             :type :variant}}}
     :sku     (m "sku")
     :name    (m "name")
     :enabled true
     :price   (-> (md "price") parse-double (* 100) int bigdec (/ 100))
     :unit    (parse-product-unit (md "sell_by"))
     :weight  (-> (md "weight") parse-double (* 100000) int bigdec (/ 100000))
     :position pos}))

(defn process-redundants [p vs k]
  (let [rds (map k vs)]
    (if (and (< 0 (count rds))
             (apply = rds))
      [(assoc-in p [:product k] (first rds))
       (map #(dissoc % k) vs)]
      [(dissoc-in p [:product k]) vs])))

(defn parse-grouped-product [m all]
  (let [p   (assoc-in (parse-single-product m)
                      [:magento :product :type] :grouped)
        chs (m "children")
        ids (map #(get % "product_id") chs)
        pos (map #(parse-long (get % "position")) chs)
        details  (map all ids)
        vars     (map parse-single-variant details pos)
        [p vars] (process-redundants p vars :price)
        [p vars] (process-redundants p vars :unit)
        [p vars] (process-redundants p vars :weight)]
    (assoc-in p [:product :variants :singles] (set vars))))

(defn parse-product-image [m]
  {:file  (m "file")
   :label (m "label")
   :position (Long/parseLong (m "position"))
   :enabled (parse-is-same (m "exclude") "0")
   :url  (str "/products/image" (m "file"))
   :tags (set (m "types"))})

(defn parse-product-images [arr]
  (->> (first arr)
       (map #(parse-product-image %))
       set))

(defn parse-product-unit [st]
  (cond (= st "242") :item
        (= st "241") :kilogram
        (= st "240") :gram
        :else :item))
