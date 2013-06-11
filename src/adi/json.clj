(defn insert- [data env]
  (emit-datoms-insert data env))

(defn insert! [conn data env]
  (d/transact conn (insert- data env)))
