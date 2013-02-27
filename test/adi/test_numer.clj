
(def i (range 10))
(def digits-map (char "i"))

(def "chris")

(char 10)

(int \a)

(defn name->numer [name]
  (let [lc-name (.toLowerCase name)
        char-vals (map #(- (inc (int %)) (int \a)) lc-name)]
    (mod (reduce + char-vals) 9)))

(defn num->numer [num] (mod num 9))

(name->numer "Chris Zheng")
(num->numer 9203434)
