(ns adi.schema.find)

(defn all-attrs
  "finds all attributes satisfying `f` in a schema

  (all-attrs
   {:account/number [{:ident   :account/number
                      :type    :long}]
    :account/date   [{:ident   :account/date
                      :type    :instant}]}
   (fn [attr] (= (:type attr) :long)))
  => {:account/number [{:type :long, :ident :account/number}]}"
  {:added "0.3"} [fschm f]
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
  {:added "0.3"} [fschm f]
  (keys (all-attrs fschm f)))
