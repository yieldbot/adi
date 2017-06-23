(ns spirit.common.atom)

(defn file-backed
  ([file]
   (file-backed (atom nil) {:file file
                            :initial {}}))
  ([atom {:keys [file initial reset]}]
   (let [initial (if reset
                   initial
                   (try (or (read-string (slurp file)) initial)
                        (catch Throwable t
                          initial)))]
     (reset! atom initial)
     (spit file (prn-str initial))
     (add-watch atom :file-backed
                (fn [_ _ _ val]
                  (spit file (prn-str val))))
     atom)))

(defn cursor
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
