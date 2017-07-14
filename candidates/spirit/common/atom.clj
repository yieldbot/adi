(ns spirit.common.atom
  (:require [spirit.common.table :as table]
            [clojure.pprint :as pprint]))

(defmulti read-value
  "reads a value from a file
   
   (read-value {:file \"test.db\"
                :format :table})
   => {:account {\"a@a.com\" {:id 1, :value \"a\"},
                 \"b@b.com\" {:id 2, :value \"b\"}},
       :info {3 {:name \"Cain\"},
              1 {:name \"Chris\"},
              2 {:name \"David\"}}}"
  {:added "0.5"}
  (fn [{:keys [format file] :as opts}] format))

(defmethod read-value :default
  [{:keys [file]}]
  (read-string (slurp file)))

(defmethod read-value :table
  [{:keys [file suffix headers levels sort-key id-key] :as opts}]
  (table/read-table opts))

(defmulti  write-value
  "write a value to file
   
   (write-value {:account {\"a@a.com\" {:id 1 :value \"a\"}
                           \"b@b.com\" {:id 2 :value \"b\"}}
                 :info {3 {:name \"Cain\"}
                       1 {:name \"Chris\"}
                        2 {:name \"David\"}}}
                {:file \"test.db\"
                 :format :table
                 :suffix \"txt\"
                 :levels 1
                 :headers  {:account [:id :email :value]
                            :info    [:id :name]}
                 :sort-key {:info    :name}
                 :id-key   {:account :email}})"
  {:added "0.5"}
  (fn [data {:keys [format file] :as opts}] format))

(defmethod write-value :default
  [data {:keys [file]}]
  (spit file (with-out-str (pprint/pprint data))))

(defmethod write-value :table
  [data {:keys [file suffix headers levels sort-key id-key] :as opts}]
  (table/write-table data opts))

(defn file-out
  "adds watch to atom, saving its contents to file on every change
 
   (def out-file (str (fs/create-tmpdir) \"/test.txt\"))
 
   (swap! (file-out (atom 1) {:file out-file})
          inc)
   
   (read-string (slurp out-file))
   => 2"
  {:added "0.5"}
  [atom {:keys [format file] :as opts}]
  (write-value @atom opts)
  (add-watch atom :file-out
             (fn [_ _ _ val]
               (write-value val opts)))
  atom)

(defn cursor
  "adds a cursor to the atom to update on any change
 
   (def a (atom {:a {:b 1}}))
   
   (def ca (cursor a [:a :b]))
 
   (do (swap! ca + 10)
       (swap! a update-in [:a :b] + 100)
       [(deref a) (deref ca)])
   => [{:a {:b 111}} 111]"
  {:added "0.5"}
  ([ref selector]
   (cursor ref selector (str (java.util.UUID/randomUUID))))
  ([ref selector key]
   (let [getter  (fn [m] (get-in m selector))
         setter  (fn [m v] (assoc-in m selector v))
         initial (getter @ref)
         cursor  (atom initial)]
     (add-watch ref key (fn [_ _ _ v]
                          (let [cv (getter v)]
                            (if (not= cv @cursor)
                              (reset! cursor cv)))))
     (add-watch cursor key (fn [_ _ _ v]
                             (swap! ref setter v)))
     cursor)))

(defn derived
  "constructs an atom derived from other atoms
 
   (def a (atom 1))
   (def b (atom 10))
   (def c (derived [a b] +))
 
   (do (swap! a + 1)
       (swap! b + 10)
       [@a @b @c])
   => [2 20 22]"
  {:added "0.5"}
  ([atoms f]
   (derived atoms f (str (java.util.UUID/randomUUID))))
  ([atoms f key]
   (let [derived-fn #(apply f (map deref atoms))
         derived  (atom (derived-fn))]
     (doseq [atom atoms]
       (add-watch atom key
                  (fn [_ _ _ _]
                    (reset! derived (derived-fn)))))
     derived)))
