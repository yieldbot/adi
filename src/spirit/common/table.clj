(ns spirit.common.table
  (:require [clojure.string :as string]
            [hara.io.file :as fs]))

(def ^:dynamic *properties-file* ".properties")

(defn generate-basic-table
  "generates a table for output
 
   (generate-basic-table [:id :value]
                         [{:id 1 :value \"a\"}
                          {:id 2 :value \"b\"}])
   
   => (ascii [\"| :id | :value |\"
              \"|-----+--------|\"
              \"|   1 |    \\\"a\\\" |\"
              \"|   2 |    \\\"b\\\" |\"])"
  {:added "0.5"}
  ([ks rows]
     (when (seq rows)
       (let [rows   (->> rows
                         (map (fn [row]
                                (reduce-kv (fn [out k v]
                                             (assoc out k (pr-str v)))
                                           {}
                                           row))))
             widths (map
                     (fn [k]
                       (apply max (count (str k)) (map #(count (get % k)) rows)))
                     ks)
             spacers (map #(apply str (repeat % "-")) widths)
             fmts (map #(str "%" % "s") widths)
             fmt-row (fn [leader divider trailer row]
                       (str leader
                            (apply str (interpose divider
                                                  (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                                    (format fmt (str col)))))
                            trailer))]
         (->> (map (fn [row] (fmt-row "| " " | " " |" row))
                   rows)
              (concat
               [(fmt-row "| " " | " " |" (zipmap ks ks))
                (fmt-row "|-" "-+-" "-|" (zipmap ks spacers))])
              (string/join "\n")))))
  ([rows] (generate-basic-table (keys (first rows)) rows)))


(defn parse-basic-table
  "reads a table from a string
 
   (parse-basic-table (ascii
                       [\"| :id | :value |\"
                        \"|-----+--------|\"
                        \"|   1 |    \\\"a\\\" |\"
                        \"|   2 |    \\\"b\\\" |\"]))
   => {:headers [:id :value]
      :data [{:id 1 :value \"a\"}
              {:id 2 :value \"b\"}]}"
  {:added "0.5"}
  [s]
  (let [[h _ & vs] (-> (string/trim-newline s)
                       (string/split-lines))
        headers    (->> (string/split h #"\|")
                        (remove empty?)
                        (map string/trim)
                        (map #(subs % 1))
                        (map keyword))
        data-fn    (fn [v]
                     (->> (string/split v #"\|")
                          (remove empty?)
                          (map string/trim)
                          (map read-string)
                          (zipmap headers)))
        data       (map data-fn vs)]
    {:headers headers
     :data data}))

(defn generate-single-table
  "generates a single table
 
   (generate-single-table {\"a@a.com\" {:id 1 :value \"a\"}
                           \"b@b.com\" {:id 2 :value \"b\"}}
                          {:headers [:id :email :value]
                           :sort-key :email
                           :id-key :email})
   => (ascii [\"| :id |    :email | :value |\"
             \"|-----+-----------+--------|\"
              \"|   1 | \\\"a@a.com\\\" |    \\\"a\\\" |\"
              \"|   2 | \\\"b@b.com\\\" |    \\\"b\\\" |\"])"
  {:added "0.5"}
  [m {:keys [id-key headers sort-key] :as opts}]
  (let [id-key (or id-key (first headers))
        rows (reduce-kv (fn [out k v]
                          (conj out (if id-key
                                      (assoc v id-key k)
                                      v)))
                        []
                        m)]
    (->> (sort-by (or sort-key id-key) rows)
         (generate-basic-table headers))))

(defn parse-single-table
  "generates a single table
 
   (parse-single-table
    (ascii [\"| :id |    :email | :value |\"
            \"|-----+-----------+--------|\"
            \"|   1 | \\\"a@a.com\\\" |    \\\"a\\\" |\"
            \"|   2 | \\\"b@b.com\\\" |    \\\"b\\\" |\"])
    
   {:headers [:id :email :value]
     :sort-key :email
     :id-key :email})
   => {\"a@a.com\" {:id 1 :value \"a\"}
       \"b@b.com\" {:id 2 :value \"b\"}}"
  {:added "0.5"}
  [s {:keys [id-key] :as opts}]
  (let [{:keys [headers data]} (parse-basic-table s)
        id-key (or id-key (first headers))]
    (reduce (fn [out m]
              (assoc out (get m id-key) (dissoc m id-key)))
            {}
            data)))

(defn write-table
  "generates a single table
 
   (write-table
    {:account {\"a@a.com\" {:id 1 :value \"a\"}
               \"b@b.com\" {:id 2 :value \"b\"}}
     :info {1 {:name \"Chris\"}
            2 {:name \"David\"}
            3 {:name \"Cain\"}}}
   {:path   \"test.db\"
     :suffix \"txt\"
     :levels 1
     :headers {:account [:id :email :value]
               :info    [:id :name]}
     :sort-key {:info :name}
     :id-key {:account :email}})
   => {:account (ascii
                 [\"| :id |    :email | :value |\"
                  \"|-----+-----------+--------|\"
                  \"|   1 | \\\"a@a.com\\\" |    \\\"a\\\" |\"
                  \"|   2 | \\\"b@b.com\\\" |    \\\"b\\\" |\"])
 
       :info (ascii
              [\"| :id |   :name |\"
               \"|-----+---------|\"
               \"|   3 |  \\\"Cain\\\" |\"
               \"|   1 | \\\"Chris\\\" |\"
               \"|   2 | \\\"David\\\" |\"])}"
  {:added "0.5"}
  [data {:keys [path suffix headers levels sort-key id-key body?]
         :as opts}]
  (if-not body? (fs/delete path))
  (if (zero? levels)
    (let [result (generate-single-table data opts)]
      (spit (str path "." suffix) result)
      result)
    (do (fs/create-directory path)
        (if-not body? (spit (str path "/" *properties-file*) (dissoc opts :path)))
        (reduce-kv (fn [out k v]
                     (let [sopts {:path (str path "/" (name k))
                                  :suffix   suffix
                                  :headers  (get headers k)
                                  :levels   (dec levels)
                                  :sort-key (get sort-key k)
                                  :id-key   (get id-key k)
                                  :body?  true}
                           result (write-table v sopts)]
                       (assoc out k result)))
                   {}
                   data))))

(defn read-table
  "generates a single table
 
   (read-table
    {:path  \"test.db\"
     :suffix \"txt\"
     :levels 1
     :headers {:account [:id :email :value]
               :info    [:id :name]}
    :sort-key {:info :name}
     :id-key {:account :email}})
   => {:account {\"a@a.com\" {:id 1 :value \"a\"}
                 \"b@b.com\" {:id 2 :value \"b\"}}
       :info {1 {:name \"Chris\"}
              2 {:name \"David\"}
              3 {:name \"Cain\"}}}"
  {:added "0.5"}
  [{:keys [path suffix headers levels sort-key id-key body?]
    :as opts}]
  (cond (fs/file? path)
        (parse-single-table (slurp path) opts)

        :else
        (let [{:keys [suffix headers levels sort-key id-key body?]
               :as opts}
              (merge (if-not body? (try (read-string (slurp (str path "/" *properties-file*)))
                                        (catch Exception e)))
                     opts)
              include (if (= levels 1) (str "*." suffix) fs/directory?)
              file-fn (if (= levels 1)
                        (fn [filename]  
                          (subs filename 0 (- (count filename)
                                              (inc (count suffix)))))
                        identity)
              files   (keys (fs/list path {:include [include]}))]
          (reduce (fn [out fullpath]
                    (let [filename (str (.getFileName (fs/path fullpath)))
                          k   (keyword  (file-fn filename))
                          val (read-table {:path     fullpath
                                           :suffix   suffix
                                           :headers  (get headers k)
                                           :levels   (dec levels)
                                           :sort-key (get sort-key k)
                                           :id-key   (get id-key k)
                                           :body?    true})]
                      (assoc out k  val)))
                  {}
                  files))))
