(ns spirit.schema.find)

(defn all-attrs
  "finds all attributes satisfying `f` in a schema

  (all-attrs
   {:account/number [{:ident   :account/number
                      :type    :long}]
    :account/date   [{:ident   :account/date
                      :type    :instant}]}
   (fn [attr] (= (:type attr) :long)))
  => {:account/number [{:type :long, :ident :account/number}]}"
  {:added "0.3"}
  [fschm f]
  (->> fschm
       (filter (fn [[k [attr]]] (f attr)))
       (into {})))

(defn all-idents
  "finds all idents satisfying `f` in a schema

  (all-idents
   {:account/number [{:ident   :account/number
                      :type    :long}]
    :account/date   [{:ident   :account/date
                      :type    :instant}]}
   (fn [attr] (= (:type attr) :long)))
  => [:account/number]"
  {:added "0.3"}
  [fschm f]
  (keys (all-attrs fschm f)))

(defn is-reverse-ref?
  "predicate for reverse ref

  (is-reverse-ref? {:ident  :email/accounts
                    :type   :ref
                    :ref    {:type :reverse}})
  => true"
  {:added "0.3"}
  [attr]
  (and (= :ref (:type attr))
       (= :reverse (-> attr :ref :type))))
