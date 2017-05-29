(ns spirit.process.emit.characterise
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [hash-map? long?]]
            [hara.data.nested :refer [merge-nested]]
            [spirit.data.common :refer [iid isym]]
            [spirit.data.checks :refer [db-id?]]))

(defn wrap-gen-id [f]
  (fn [pdata fns spirit]
    (let [id  (get-in pdata [:# :id])
          nid (cond (nil? id)
                    (let [gen  (-> spirit :options :generate-ids)]
                      (if (fn? gen) (gen) (iid)))

                    (symbol? id) (iid id)

                    :else id)]
      (swap! (:tempids spirit) conj nid)
      (f (assoc-in pdata [:# :id] nid) fns spirit))))

(defn wrap-gen-sym [f]
  (fn [pdata fns spirit]
    (if (nil? (get-in pdata [:# :sym]))
      (let [gen    (-> spirit :options :generate-syms)
            sym (if (fn? gen) (gen) (isym))]
        (assoc-in (f pdata fns spirit) [:# :sym] sym))
      (f pdata fns spirit))))

(defn wrap-reverse [f is-reverse pid attr spirit]
  (fn [x]
    (let [res (f x)]
      (if is-reverse
        (-> res
            (assoc-in [:# :rid] pid)
            (assoc-in [:# :rkey] (-> attr :ref :rkey)))
        res))))

(defn characterise-ref-single [k v pid attr fns spirit output]
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
                f    (wrap-reverse #((:characterise fns) % fns spirit)
                                   is-reverse pid attr spirit)
                res  (f v)
                output (assoc-in output [cat-data lu] res)]
            output))))

(defn characterise-ref-many [k vs pid attr fns spirit output]
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
                                          #((:characterise fns) % fns spirit)
                                          is-reverse pid attr spirit)
                                         all-maps))))]
    output))

(defn characterise-entry [k v pid fns spirit output]
  (let [[attr] (-> spirit :schema :flat k)
        t (:type attr)]
    (if (and attr t)
      (cond (list? v) (assoc-in output [:db-funcs k] v)

            (and (set? v) (get v '_)) (assoc-in output [:data-many k] #{'_})

            (not= :ref t)
            (cond
             (set? v)  (assoc-in output [:data-many k] v)
             :else     (assoc-in output [:data-one k] v))

            (= :ref t)
            (cond (set? v) (characterise-ref-many k v pid attr fns spirit output)
                  :else (characterise-ref-single k v pid attr fns spirit output)))

      (cond (= k :db)
            (assoc output k v)

            (= k :#)
            (assoc output k (merge-nested (output k) v))

            :else
            (error "key " k " not found in schema.")))))


(defn characterise-loop
  ([pdata fns spirit]
     (let [id (get-in pdata [:# :id])]
       (characterise-loop pdata id fns spirit {})))
  ([pdata id fns spirit output]
     (if-let [[k v] (first pdata)]
      (recur (next pdata) id fns spirit
             (characterise-entry k v id fns spirit output))
      output)))

(defn characterise-raw
  [pdata spirit]
  (let [fns {:characterise
             (let [f characterise-loop
                   f (cond (and (= "query" (:type spirit))
                                (not (false? (-> spirit :options :generate-syms))))
                           (wrap-gen-sym f)

                           (and (= "datoms" (:type spirit))
                                (not (false? (-> spirit :options :generate-ids))))
                           (wrap-gen-id f)

                           :else f)]
               f)}]
    ((:characterise fns) pdata fns spirit)))

(defn characterise [spirit]
  (let [data (-> spirit :process :reviewed)
        ndata (characterise-raw data spirit)]
    (assoc-in spirit [:process :characterised] ndata)))
