(ns crystaluniverse.parsing-test
  (:use midje.sweet)
  (:require [crystaluniverse.datamaps :as dm]
            [crystaluniverse.parsing :as p]
            [fs.core :as fs]
            [cheshire.core :as json]
            [adi.api :as aa]
            [adi.core :as adi]
            [datomic.api :as d]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]))


(def single-occo
  (-> "php/grouped/999.json"
      slurp
      json/parse-string))


(def creation-time (get-in single-occo ["details" 0 "created_at"]))

(tc/to-string (t/now))
(tc/from-string )
(:history (p/parse-single-product single-occo))








;;; Testing grouped products

(def all-maps
  (->> (range 729 736)
       (map #(str "php/grouped/" % ".json"))
       (map #(-> % slurp json/parse-string))))

(def group-map (last all-maps))
(def all-maps*
  (zipmap  (map #(% "product_id") all-maps)
           all-maps))

(spit "windchimes.json"
      (json/generate-string (p/parse-grouped-product group-map all-maps*)))

(-> pm)


()

(def ^:dynamic *ds* (adi/datastore "datomic:mem://magento.output"
                         dm/all true true))

;;(:history/added (:fschm *ds*))

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


;; Installing the Data
(install-categories *ds* "php/categories.json")
(def ppd (parse-products "php/grouped"))
(adi/insert! *ds* ppd)



;; Generating
(defn replace-all [x b a]
  (.replaceAll x b a))

(defn clean-name [n]
  (clojure.string/join
   "-" (-> (replace-all n "&" "and")
           (replace-all "'" "")
           clojure.string/lower-case
           (clojure.string/split #" "))))

(defn get-all-leaf-categories [ds]
  #_(adi/q ds '[:find ?cname ?cid :where
                     [?e :category/name ?cname]
                     [?ec :category/children ?c]
                     [(not= ?ec ?e)]
              [?e :magento/category/id ?cid]] [])
  (clojure.set/difference
   (set (adi/q ds '[:find ?cname ?cid :where
                      [?e :category/name ?cname]
                      [?e :magento/category/id ?cid]] []))
   (set (adi/q ds '[:find ?cname ?cid :where
                      [?e :category/name ?cname]
                      [?e :category/children ?c]
                      [?e :magento/category/id ?cid]
                      ] []))))

(get-all-leaf-categories *ds*)

;; Generating
(defn get-category-products [ds id]
  (adi/select ds {:magento/product/categories id
                    :#/not {:magento/product/type :variant}}
              (aa/emit-refroute (:fschm ds))))

(defn spit-category-products-json [ds [n id]]
  (spit (str "outputs/" id "-"(clean-name n) ".json")
        (-> (get-category-products ds id)
            json/generate-string)))

(defn gen-category-urls [ds [n id]]
  (let [ps (get-category-products ds id)]
    (concat
     [(str "/#!/catalogue/" (clean-name n) "/list")
      (str "/#!/catalogue/" (clean-name n) "/detailed")]
     (map #(str "/#!/catalogue/" (clean-name n) "/product/" (-> % :product :slug)) ps))))

(defn gen-all-urls [ds]
  (let [cats (get-all-leaf-categories ds)]
    (mapcat #(gen-category-urls ds %) cats)))


;; generate all urls
(spit "urls.json"
      (json/generate-string (gen-all-urls *ds*)))

;; generate all category outputs
(doseq [c (get-all-leaf-categories *ds*)]
  (spit-category-products-json *ds* c))
