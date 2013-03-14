(+ 1 1)

(defn add1 [x]
  (+ x 1))

(println (add1 5))

(def add1
  (fn [x] (+ x 1)))

(def addn
  (fn [n]
    (fn [x]
      (+ x n))))


(def add1 (addn 1))
(def add9 (addn 9))

(println (add9 9))

(map #(* 2 %) [1 2 3 4 5 6])

(map (addn 10) [1 2 3 4 5 6])
