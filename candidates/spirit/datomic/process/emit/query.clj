(ns spirit.process.emit.query
  (:require [hara.common
             [error :refer [error]]
             [checks :refer [hash-map?]]]
            [spirit.data.common :refer [isym]]))

(defn walk-replace [st rep]
  (cond (vector? st) (mapv #(walk-replace % rep) st)
        (list? st) (map #(walk-replace % rep) st)
        (hash-map? st) (zipmap (keys st)
                               (map #(walk-replace % rep) (vals st)))
        (rep st) (rep st)
        :else st))

(defn query-sym [chdata]
  (get-in chdata [:# :sym]))

(def ?init '[??sym ??attr ?])

(defn has-placeholder? [lst]
  (some #(= '? %) lst))

(defn q-fn [[x & xs :as lst]]
  (if (has-placeholder? lst)
    [?init [lst]]
    [?init [(apply list x '? xs)]]))

(defn not-fn [val]
  [?init [(list 'not= '? val)]])

(defn fulltext-fn [val]
  [[(list 'fulltext '$ '??attr val) [['??sym '?]]]])

(defn query-parse-list [[x & xs :as lst]]
  (cond
   (= x '?fulltext) (fulltext-fn (second lst))
   (= x '?not) (not-fn (second lst))
   :else (q-fn lst)))

(defn query-replace-fn [sym k v spirit]
  (let [gen (-> spirit :options :generate-syms)
        symgen (if (fn? gen) gen isym)
        esym (symgen)]
    (walk-replace v {'??sym sym '? esym '??attr k})))

(defn query-data-val [sym k v spirit]
  (cond (list? v)
        (query-replace-fn sym k (query-parse-list v) spirit)

        :else
        [[sym k v]]))

(defn query-data [chdata spirit]
  (let [sym  (query-sym chdata)
        data (for [[k vs] (:data-many chdata)
                   v     vs]
               (query-data-val sym k v spirit))]
    (apply concat data)))

(defn query-refs [chdata]
  (let [sym (query-sym chdata)]
    (concat
     (filter identity
             (for [[k rs] (:refs-many chdata)
                   r   rs]
               (if-let [rid (get-in r [:# :id])]
                 [sym k rid])))
     (for [[k rs] (:refs-many chdata)
           r   rs]
       [sym k (query-sym r)])
     (filter identity
             (for [[k rs] (:revs-many chdata)
                   r   rs]
               (if-let [rid (get-in r [:# :id])]
                 [rid k sym])))
     (for [[k rs] (:revs-many chdata)
           r   rs]
       [(query-sym r) k sym])
     (for [[k ids] (:ref-ids chdata)
           id ids]
       [sym k id])
     (for [[k ids] (:rev-ids chdata)
           id ids]
       [id k sym]))))

(defn query-init [chdata spirit]
  (cond
   (nil? (seq chdata)) []
   :else
   (concat  (query-data chdata spirit)
            (query-refs chdata)
            (mapcat (fn [x]
                      (mapcat #(query-init % spirit) (second x)))
                    (concat (:refs-many chdata) (:revs-many chdata))))))

(defn query-raw
  [chdata spirit]
  (let [res (query-init chdata spirit)]
    (if (empty? res)
      (error "QUERY The generated query is empty for " chdata))
    (vec (concat [:find (query-sym chdata) :where]
                 res))))

(defn query [spirit]
  (let [chdata (-> spirit :process :characterised)
        ndata (query-raw chdata spirit)]
    (assoc-in spirit [:process :emitted] ndata)))
