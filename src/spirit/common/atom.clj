(ns spirit.common.atom
  (:require [spirit.common.table :as table]
            [hara.io.file :as fs]
            [clojure.pprint :as pprint]))

(defmulti read-value
  "reads a value from a file
   
   (read-value {:path \"test.db\"
                :format :table})
   => {:account {\"a@a.com\" {:id 1, :value \"a\"},
                 \"b@b.com\" {:id 2, :value \"b\"}},
       :info {3 {:name \"Cain\"},
              1 {:name \"Chris\"},
              2 {:name \"David\"}}}"
  {:added "0.5"}
  (fn [{:keys [format path] :as opts}] format))

(defmethod read-value :default
  [{:keys [path] :as opts}]
  (read-string (slurp path)))

(defmethod read-value :table
  [{:keys [path suffix headers levels sort-key id-key] :as opts}]
  (table/read-table opts))

(defmulti  write-value
  "write a value to file
   
   (write-value {:account {\"a@a.com\" {:id 1 :value \"a\"}
                           \"b@b.com\" {:id 2 :value \"b\"}}
                 :info {3 {:name \"Cain\"}
                       1 {:name \"Chris\"}
                        2 {:name \"David\"}}}
                {:path \"test.db\"
                 :format :table
                 :suffix \"txt\"
                 :levels 1
                 :headers  {:account [:id :email :value]
                            :info    [:id :name]}
                 :sort-key {:info    :name}
                 :id-key   {:account :email}})"
  {:added "0.5"}
  (fn [data {:keys [format path] :as opts}] format))

(defmethod write-value :default
  [data {:keys [path] :as opts}]
  (spit path (with-out-str (pprint/pprint data))))

(defmethod write-value :table
  [data {:keys [path suffix headers levels sort-key id-key] :as opts}]
  (table/write-table data opts))

(defn file-out
  "adds watch to atom, saving its contents to file on every change
 
   (def out-file (str (fs/create-tmpdir) \"/test.txt\"))
   
   (swap! (file-out (atom 1) {:path out-file})
          inc)
   
   (read-string (slurp out-file))
   => 2"
  {:added "0.5"}
  [atom {:keys [transform] :as opts :or {transform identity}}]
  (write-value (transform @atom) opts)
  (add-watch atom :watch/file-out
             (fn [_ _ _ val]
               (write-value (transform val) opts)))
  atom)

(defn log-out
  "adds watch to atom, logging the contents on every change
   
   (with-out-str
     (swap! (log-out (atom 1) {})
            inc))"
  {:added "0.5"}
  [atom {:keys [transform] :as opts :or {transform identity}}]
  (add-watch atom :watch/log-out
             (fn [_ _ _ val]
               (println "LOG:")
               (pprint/pprint (transform val)))))

(defn attach-state
  "used with component, adds watch on record that incorporates state"
  {:added "0.5"}
  [{:keys [state initial file log] :as struct}]
  (let [state (or state (atom (or initial {})))]
    (when file
      (if (:reset file) (fs/delete (:path file)))
      (file-out state file))
    (when log
      (log-out state log))
    (assoc struct :state state)))

(defn detach-state
  "used with component, remove watch on record that incorporates state"
  {:added "0.5"}
  [{:keys [state file log] :as struct}]
  (when file
    (if (:cleanup file) (fs/delete (:path file)))
    (remove-watch state :watch/file-out))
  (when log
    (remove-watch state :watch/log-out))
  (assoc struct :state nil))

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
