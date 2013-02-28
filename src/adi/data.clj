(ns adi.data
  (:use [datomic.api :only [tempid]]
        [adi.schema :as as]
        adi.utils))

(defn iid
  "Constructs a new id"
  ([] (tempid :db.part/user))
  ([obj]
     (let [v (hash obj)
           v (if (< 0 v) (- v) v)]
       (tempid :db.part/user v ))))

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

(defn try-keys [k data]
  (let [kns (seperate-keys k)
        trials [[:+ k] (cons :+ kns) [k] kns]]
    (->> trials
         (filter #(get-in data %))
         first)))

(defn get-id [val]
  (cond (integer? val) val

         (ref? val)
         (if-let [kns (try-keys :db/id val)]
           ((merge-keys kns) val))

         :else (throw (Exception. (str "Not an integer, entity nor hashmap")))))


(declare process
         process-assoc process-ref process-default)

(defn process
  "Processes the data according to the schema specified to form a tree-like
   structure of refs and values for the next step of characterisation."
  ([fsm data] (process fsm data true))
  ([fsm data defaults?] (process fsm fsm data {} defaults?))
  ([fsm nfsm data output defaults?]
     (if-let [[k [meta]] (first nfsm)]
       (let [tk     (try-keys k data)
             v      (cond tk (get-in data tk)
                          defaults? (process-default meta k data))
             output (if v
                      (process-assoc fsm meta output k v defaults?)
                      output)]
         (process fsm (rest nfsm) data output defaults?))
       (if-let [ks (try-keys :db/id data)]
         (assoc output :db/id (get-in data ks))
         output))))

(defn- process-default [meta k data]
  (let [n  (seperate-keys (key-ns k))
        m (treeify-keys data)]
    (if (get-in m n) (:default meta))))

(defn- process-ref [fsm meta v defaults?]
  (let [kns   (seperate-keys (:ref-ns meta))
        refv (extend-key-ns v kns [:+])]
    (process fsm refv defaults?)))

(defn- process-assoc [fsm meta output k v defaults?]
  (if (correct-type? meta v)
    (let [t (:type meta)
          c (or (:cardinality meta) :one)]
      (cond (not= t :ref)
            (assoc output k v)

            (= c :one)
            (assoc output k (process-ref fsm meta v defaults?))

            (= c :many)
            (assoc output k (set (map #(process-ref fsm meta % defaults?) v)))))))

(declare unprocess
         unprocess-assoc unprocess-ref)

(defn unprocess
  "The opposite of process. Takes a map and turns it into a nicer looking
   data-structure"
  ([fsm pdata rset]
     (if-let [id (:db/id pdata)]
       (unprocess fsm pdata rset {:db/id id} #{id})
       (unprocess fsm pdata rset {} #{})))
  ([fsm pdata rset exclude]
     (unprocess fsm pdata rset {} exclude))
  ([fsm pdata rset output exclude]
     (if-let [[k v] (first pdata)]
       (if-let [[meta] (k fsm)]
         (unprocess fsm
                         (next pdata)
                         rset
                         (unprocess-assoc fsm rset meta output k v exclude)
                         exclude)
         (unprocess fsm (next pdata) rset output exclude))
       output)))

(defn- unprocess-assoc [fsm rset meta output k v exclude]
  (if (correct-type? meta v)
    (let [t (:type meta)
          c (or (:cardinality meta) :one)
          kns (seperate-keys k)]
      (cond (not= t :ref)
            (assoc-in output kns v)

            (= c :one)
            (assoc-in output kns (unprocess-ref fsm rset meta k v exclude))

            (= c :many)
            (assoc-in output kns
                      (set (map #(unprocess-ref fsm rset meta k % exclude) v)))))))

(defn- unprocess-ref [fsm rset meta k v exclude]
  (let [id (:db/id v)]
      (cond (exclude id)
            {:+ {:db/id id}}

            (rset k)
            (let [nks (seperate-keys (:ref-ns meta))
                  nm  (unprocess fsm v rset)
                  cm  (get-in nm nks)
                  xm  (dissoc-in nm nks)]
              (if (empty? xm) cm
                  (merge cm (assoc {} :+ xm))))

            :else
            (if id {:+ {:db/id id}} {}))))

(defn characterise
  "Characterises the data into datomic specific format so that converting
   into datomic datastructures become easy."
  ([fsm pdata] (characterise fsm pdata {}))
  ([fsm pdata output]
     (if-let [[k v] (first pdata)]
       (let [t (-> fsm k first :type)]
         (cond (= k :db/id)
               (characterise fsm (next pdata) (assoc output k v))

               (and (set? v) (= :ref t))
               (characterise fsm (next pdata)
                             (assoc-in output
                                       [:refs-many k]
                                       (set (map #(characterise fsm %) v))))
               (set? v)
               (characterise fsm (next pdata) (assoc-in output [:data-many k] v))

               (= :ref t)
               (characterise fsm (next pdata)
                             (assoc-in output
                                       [:refs-one k]
                                       (characterise fsm v)))

               :else
               (characterise fsm (next pdata) (assoc-in output [:data-one k] v))))
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
;;  EMIT FUNCTIONS
;;
;;                              emit
;;               *----------------------------------*
;;  fsm, data -> | process -> characterize -> build | -> datomic structures
;;               *----------------------------------*
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn emit
  "Generates the datomic data given a datamap and refs"
  ([fsm data]
    (cond (hash-map? data)
          (->> (process fsm data)
               (characterise fsm)
               build)
          (vector? data)
          (apply emit fsm data)))
  ([fsm data & more]
     (concat (emit fsm data)
             (apply emit fsm more))))

(defn emit-no-defaults
  "Generates the datomic data given a datamap and refs with no defaulst"
  ([fsm data]
    (cond (hash-map? data)
          (->> (process fsm data false)
               (characterise fsm)
               build)
          (vector? data)
          (apply emit fsm data)))
  ([fsm data & more]
     (concat (emit-no-defaults fsm data)
             (apply emit-no-defaults fsm data more))))
