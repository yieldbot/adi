(ns anadi.data
  (:use [datomic.api :only [tempid]]
        [anadi.schema :as sm]
        anadi.utils))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (tempid :db.part/user (hash obj))))

(defn correct-type? [meta v]
  "Checks to see if v matches the description in the meta.
   throws an exception if the v does not"
  (let [t (:type meta)
        c (or (:cardinality meta) :one)
        chk (type-checks t)]
    (cond (and (= c :one) (not (chk v)))
          (throw (Exception. (format "The value %s is not of type %s" v t)))
          (and (= c :many) (not (set v)))
          (throw (Exception. (format "%s needs to be a set" v)))
          (and (= c :many) (not (every? chk v)))
          (throw (Exception. (format "Not every value in %s is not of type %s" v t))))
    true))

(defn trial-keys [k data]
  (let [kns (seperate-keys k)
        trials [[:+ k] (cons :+ kns) [k] kns]]
    (->> trials
         (filter #(get-in data %))
         first)))


(declare process-data
         process-assoc process-ref process-default)

(defn process-data
  "Processes the data according to the schema specified to form a tree-like
   structure of refs and values for the next step of characterisation."
  ([fm data] (process-data fm data true))
  ([fm data defaults?] (process-data fm fm data {} defaults?))
  ([fm nfm data output defaults?]
     (if-let [[k [meta]] (first nfm)]
       (let [tk     (trial-keys k data)
             v      (cond tk (get-in data tk)
                          defaults? (process-default meta k data))
             output (if v
                      (process-assoc fm meta output k v defaults?)
                      output)]
         (process-data fm (rest nfm) data output defaults?))
       (if-let [ks (trial-keys :db/id data)]
         (assoc output :db/id (get-in data ks))
         output))))

(defn process-default [meta k data]
  (let [n  (seperate-keys (key-ns k))
        m (treeify-keys data)]
    (if (get-in m n) (:default meta))))

(defn process-ref [fm meta v defaults?]
  (let [kns   (seperate-keys (:ref-ns meta))
        refv (extend-key-ns v kns [:+])]
    (process-data fm refv defaults?)))

(defn process-assoc [fm meta output k v defaults?]
  (if (correct-type? meta v)
    (let [t (:type meta)
          c (or (:cardinality meta) :one)]
      (cond (not= t :ref)
            (assoc output k v)

            (= c :one)
            (assoc output k (process-ref fm meta v defaults?))

            (= c :many)
            (assoc output k (set (map #(process-ref fm meta % defaults?) v)))))))

(declare deprocess-data
         deprocess-assoc process-ref)

(defn characterise
  "Characterises the data into datomic specific format so that converting
   into datomic datastructures become easy."
  ([fm pdata] (characterise fm pdata {}))
  ([fm pdata output]
     (if-let [[k v] (first pdata)]
       (let [t (-> fm k first :type)]
         (cond (= k :db/id)
               (characterise fm (next pdata) (assoc output k v))

               (and (set? v) (= :ref t))
               (characterise fm (next pdata)
                             (assoc-in output
                                       [:refs-many k]
                                       (set (map #(characterise fm %) v))))
               (set? v)
               (characterise fm (next pdata) (assoc-in output [:data-many k] v))

               (= :ref t)
               (characterise fm (next pdata)
                             (assoc-in output
                                       [:refs-one k]
                                       (characterise fm v)))

               :else
               (characterise fm (next pdata) (assoc-in output [:data-one k] v))))
       (if (:db/id output)
         output
         (assoc output :db/id (iid))))))

(declare build
         build-data-one build-data-many
         build-refs-one build-refs-many)

(defn build
  "Builds the datomic query structure from the
  characterised result"
  ([chdata]
    (concat (build chdata build-data-one build-data-many)
            (build chdata build-refs-one build-refs-many)))
  ([chdata f1 f2]
    (cond (nil? (seq chdata)) []
        :else
        (concat (mapcat (fn [x] (build (second x) f1 f2)) (:refs-one chdata))
                (mapcat (fn [x]
                          (mapcat #(build % f1 f2) (second x))) (:refs-many chdata))
                (f1 chdata)
                (f2 chdata)))))

;; Build Helper Functions

(defn- build-data-one [chd]
  [(assoc (:data-one chd) :db/id (:db/id chd))])

(defn- build-data-many [chd]
  (for [[k vs] (:data-many chd)
        v vs]
    [:db/add (:db/id chd) k v]))

(defn- build-refs-one [chd]
  (for [[k ref] (:refs-one chd)]
    [:db/add (:db/id chd) k (:db/id ref)]))

(defn- build-refs-many [chd]
  (for [[k refs] (:refs-many chd)
        ref refs]
    [:db/add (:db/id chd) k (:db/id ref)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  GENERATE-DATA FUNCTIONS
;;
;;
;;              *---------------------------------------*
;;  fm, data -> | process-data -> characterize -> build | -> datomic structures
;;              *---------------------------------------*
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn generate-data
  "Generates the datomic data given a datamap and refs"
  ([fm data]
     (->> (process-data fm data)
          (characterise fm)
          build))
  ([fm data & more]
     (concat (generate-data fm data) (apply generate-data fm more))))
