(ns crystaluniverse.output
  (:require [crystaluniverse.datamaps :as dm]
            [crystaluniverse.parsing :as p]
            [fs.core :as fs]
            [cheshire.core :as json]
            [adi.api :as aa]
            [adi.core :as adi]
            [datomic.api :as d]))

(def *ds* (adi/datastore "datomic:mem://magento.output"
                         dm/all true true))

;; Install Categories
(defn install-categories [ds file]
  (let [c-json (slurp file)
        c-clj  (json/parse-string c-json)
        c-data (p/parse-all-categories c-clj)]
    (adi/insert! ds c-data)))

(defn parse-products [p-dir]
  (let [p-files (map #(str p-dir "/" %)
                    (fs/list-dir p-dir))
        p-clj   (->> p-files
                    (map slurp)
                    (map json/parse-string))
        p-ids   (map #(get % "product_id") p-clj)
        p-all   (zipmap p-ids p-clj)]
    (p/parse-products p-all)))


(def ppd (parse-products "php/grouped"))
(pprint (aa/emit-insert (take 1 ppd) (:fschm *ds*)))

(type ppd)
(pprint (take 1 ppd))
(adi/insert! *ds* ppd)
(install-categories *ds* "php/categories.json")
;;(install-products *ds* "php/grouped")

(def a (install-products *ds* "php/grouped"))

(pprint (first a))

(adi/select *ds* {:category/name "Crystal Universe"} (aa/emit-refroute (:fschm *ds*)))

(aa/emit-query {:magento {:product {:categories 46}}
                :product {:variants {:groups {:sku '_}}}}  (:fschm *ds*))

(get-in (:fschm *ds*) [:magento/product/active])

(count
 (adi/select *ds*  {:magento {:product {:categories 46
                                        :type :grouped}}}
             (aa/emit-refroute (:fschm *ds*))))

(count
 (adi/select *ds*  {:magento {:product {:categories 46
                                        :type :single}}}
             (aa/emit-refroute (:fschm *ds*))))

(count
 (adi/select *ds*  {:magento {:product {:type :variant}}}
             (aa/emit-refroute (:fschm *ds*))))



(def leaf-categories
 (clojure.set/difference
  (set (adi/q *ds* '[:find ?cname ?cid :where
                     [?e :category/name ?cname]
                     [?e :magento/category/id ?cid]] []))
  (set (adi/q *ds* '[:find ?cname ?cid :where
                     [?e :category/name ?cname]
                     [?e :category/children ?c]
                     [?e :magento/category/id ?cid]
                     ] []))))

(count
 (adi/select *ds* {:magento/product/categories 18
                   :#/not {:magento/product/type :variant}}
             (aa/emit-refroute (:fschm *ds*))))

(adi/select *ds* {:magento/product/categories 18
                  :magento/product/type :variant}
             (aa/emit-refroute (:fschm *ds*)))

(defn replace-all [x b a]
  (.replaceAll x b a))

(defn clean-name [n]
  (clojure.string/join
   "-" (-> (replace-all n "&" "and")
           (replace-all "'" "")
           clojure.string/lower-case
           (clojure.string/split #" "))))

(.replaceAll "oeuoeu" "o" "e")

(defn spit-json [[n id]]
  (spit (str "outputs/" id "-"(clean-name n) ".json")
        (json/generate-string
         (adi/select *ds* {:magento/product/categories id
                           :#/not {:magento/product/type :variant}}
                     (aa/emit-refroute (:fschm *ds*))))))

()

(doseq [c leaf-categories]
  (spit-json c))




(spit "category-info.json"
      (json/generate-string
       (adi/select *ds* {:category/name "Crystal Universe"} (aa/emit-refroute (:fschm *ds*)))))













































)
