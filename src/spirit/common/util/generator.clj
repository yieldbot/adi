(ns spirit.common.util.generator)

(defn incremental-sym-gen
  "constructs a function that generate incremental symbols 
  when repeatedly called.
  (repeatedly 5 (incremental-sym-gen 'e)) 
  => '(?e1 ?e2 ?e3 ?e4 ?e5)"
  {:added "0.3"}
  ([s] (incremental-sym-gen s 0))
  ([s n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         (symbol (str "?" s @r))))))

(defn incremental-id-gen
  "constructs a function that generate incremental ids 
  when repeatedly called.
  (repeatedly 5 (incremental-id-gen 100)) 
  => [101 102 103 104 105]"
  {:added "0.3"}
  ([] (incremental-id-gen 0))
  ([n]
     (let [r (atom n)]
       (fn []
         (swap! r inc)
         @r))))
