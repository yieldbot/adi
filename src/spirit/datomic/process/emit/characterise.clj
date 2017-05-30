(ns spirit.datomic.process.emit.characterise
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [hash-map? long?]]
            [hara.data.nested :refer [merge-nested]]
            [spirit.datomic.data :refer [iid isym]]
            [spirit.datomic.data.checks :refer [db-id?]]))

(defn wrap-gen-id [f]
  (fn [pdata fns datasource]
    (let [id  (get-in pdata [:# :id])
          nid (cond (nil? id)
                    (let [gen  (-> datasource :options :generate-ids)]
                      (if (fn? gen) (gen) (iid)))

                    (symbol? id) (iid id)

                    :else id)]
      (swap! (:tempids datasource) conj nid)
      (f (assoc-in pdata [:# :id] nid) fns datasource))))

(defn wrap-gen-sym [f]
  (fn [pdata fns datasource]
    (if (nil? (get-in pdata [:# :sym]))
      (let [gen    (-> datasource :options :generate-syms)
            sym (if (fn? gen) (gen) (isym))]
        (assoc-in (f pdata fns datasource) [:# :sym] sym))
      (f pdata fns datasource))))

(defn wrap-reverse [f is-reverse pid attr datasource]
  (fn [x]
    (let [res (f x)]
      (if is-reverse
        (-> res
            (assoc-in [:# :rid] pid)
            (assoc-in [:# :rkey] (-> attr :ref :rkey)))
        res))))

(defn characterise-ref-single [k v pid attr fns datasource output]
  (let [is-reverse (not= k (-> attr :ref :key))
        lu         (if is-reverse (-> attr :ref :rkey) (-> attr :ref :key))
        cat-data   (if is-reverse :revs-one :refs-one)
        cat-id     (if is-reverse :rev-ids-one :ref-ids-one)]
    (cond (or (long? v) (db-id? v) (symbol? v))
          (assoc-in output
                    [(if is-reverse :rev-ids :ref-ids) lu]
                    #{v})

          (hash-map? v)
          (let [id   (get-in v [:# :id])
                f    (wrap-reverse #((:characterise fns) % fns datasource)
                                   is-reverse pid attr datasource)
                res  (f v)
                output (assoc-in output [cat-data lu] res)]
            output))))

(defn characterise-ref-many [k vs pid attr fns datasource output]
  (let [is-reverse  (not= k (-> attr :ref :key))
        lu         (if is-reverse (-> attr :ref :rkey) (-> attr :ref :key))
        cat-data   (if is-reverse :revs-many :refs-many)
        cat-id  (if is-reverse :rev-ids-many :ref-ids-many)
        id-pred  (fn [x] (or (long? x) (db-id? x) (symbol? x)))
        all-maps (filter (complement id-pred) vs)
        map-ids  (->> all-maps
                      (map #(get-in % [:# :id])))
        all-ids  (->> vs
                      (filter id-pred))
        output   (if (empty? all-ids) output
                     (-> output
                         (assoc-in [cat-id lu]
                                   (set (filter identity map-ids)))
                         (assoc-in [(if is-reverse :rev-ids :ref-ids) lu]
                                   (set all-ids))))
        output   (if (empty? all-maps) output
                     (assoc-in output [cat-data lu]
                               (set (map (wrap-reverse
                                          #((:characterise fns) % fns datasource)
                                          is-reverse pid attr datasource)
                                         all-maps))))]
    output))

(defn characterise-entry [k v pid fns datasource output]
  (let [[attr] (-> datasource :schema :flat k)
        t (:type attr)]
    (if (and attr t)
      (cond (list? v) (assoc-in output [:db-funcs k] v)

            (and (set? v) (get v '_)) (assoc-in output [:data-many k] #{'_})

            (not= :ref t)
            (cond
             (set? v)  (assoc-in output [:data-many k] v)
             :else     (assoc-in output [:data-one k] v))

            (= :ref t)
            (cond (set? v) (characterise-ref-many k v pid attr fns datasource output)
                  :else (characterise-ref-single k v pid attr fns datasource output)))

      (cond (= k :db)
            (assoc output k v)

            (= k :#)
            (assoc output k (merge-nested (output k) v))

            :else
            (error "key " k " not found in schema.")))))


(defn characterise-loop
  ([pdata fns datasource]
     (let [id (get-in pdata [:# :id])]
       (characterise-loop pdata id fns datasource {})))
  ([pdata id fns datasource output]
     (if-let [[k v] (first pdata)]
      (recur (next pdata) id fns datasource
             (characterise-entry k v id fns datasource output))
      output)))

(defn characterise-raw
  [pdata datasource]
  (let [fns {:characterise
             (let [f characterise-loop
                   f (cond (and (= "query" (:type datasource))
                                (not (false? (-> datasource :options :generate-syms))))
                           (wrap-gen-sym f)

                           (and (= "datoms" (:type datasource))
                                (not (false? (-> datasource :options :generate-ids))))
                           (wrap-gen-id f)

                           :else f)]
               f)}]
    ((:characterise fns) pdata fns datasource)))

(defn characterise [datasource]
  (let [data (-> datasource :process :reviewed)
        ndata (characterise-raw data datasource)]
    (assoc-in datasource [:process :characterised] ndata)))
