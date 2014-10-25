(ns adi.schema.test-emit
  (:use midje.sweet
        adi.schema.emit)
  (:require [adi.schema.emit :as t]
            [adi.schema.meta :as m]
            [adi.schema.xm :as x]))

(fact "find-revrefs-idents"
  (find-revref-idents
    (x/fschm-prepare-ischm
      {:account/email [{:ident   :account/email
                        :type    :ref
                        :ref     {:ns  :email}}]}))
  => '(:email/accounts))

(fact "remove-revref-attrs"
  (remove-revref-attrs
   (x/fschm-prepare-ischm
     {:account/email [{:ident   :account/email
                       :type    :ref
                       :ref     {:ns  :email}}]}))
  => (just {:account/email vector?}))

(fact "remove-enum-attrs"
  (remove-enum-attrs
    {:person/gender [{:ident   :person/gender
                      :type    :enum
                      :enum    {:ns     :person.gender
                                        :values #{:male  :female}}}]})
  => {:person/gender   [{:ident :person/gender
                         :type  :ref
                         :ref   {:ns     :person.gender
                                 :values #{:male  :female}
                                 :type   :enum-rel}}]})

(fact "emit-dschm-attribute-property"
 (emit-dschm-attr-property {:type :string} :type (m/meta-schema :type) {})
 => {:db/valueType :db.type/string}

 (emit-dschm-attr-property {:cardinality :one} :cardinality (m/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (emit-dschm-attr-property {} :cardinality (m/meta-schema :cardinality) {})
 => {:db/cardinality :db.cardinality/one}

 (emit-dschm-attr-property {} :unique (m/meta-schema :unique) {})
 => {}

 (emit-dschm-attr-property {} :type (m/meta-schema :type) {})
 => {:db/valueType :db.type/string}

 (emit-dschm-attr-property {:type :ERROR} :type (m/meta-schema :type) {})
 => (throws Exception))


(fact "emit-dschm-attr"
 (emit-dschm-attr [{:ident :name
                        :type  :string}])
   => (contains {:db.install/_attribute :db.part/db,
                 :db/ident :name,
                 :db/valueType :db.type/string,
                 :db/cardinality :db.cardinality/one})

   (emit-dschm-attr [{:ident       :account/tags
                         :type        :string
                         :cardinality :many
                         :fulltext    true
                         :index       true
                         :doc         "tags for account"}])
   => (contains {:db.install/_attribute :db.part/db
                 :db/ident        :account/tags
                 :db/index        true
                 :db/doc          "tags for account"
                 :db/valueType    :db.type/string
                 :db/fulltext     true
                 :db/cardinality  :db.cardinality/many}))


 (fact "emit-dschm"
   (emit-dschm (x/fschm-prepare-ischm {:node/male/node   [{:type  :ref :ref-ns  :node}]
                                   :node/female/node [{:type  :ref :ref-ns  :node}]}))
   => (just
       [(contains {:db/ident              :node/male/node
                   :db/valueType          :db.type/ref
                   :db/cardinality        :db.cardinality/one})
        (contains {:db/ident              :node/female/node
                   :db/valueType          :db.type/ref
                   :db/cardinality        :db.cardinality/one})])


   (emit-dschm  {:person/gender [{:ident   :person/gender
                                   :type    :enum
                                   :enum    {:ns     :person.gender
                                             :values #{:male  :female}}}]})
   => (just [(just {:db/id anything
                    :db.install/_attribute :db.part/db
                    :db/ident       :person/gender
                    :db/cardinality :db.cardinality/one
                    :db/valueType   :db.type/ref})
             (just {:db/id anything
                    :db/ident       :person.gender/male})
             (just {:db/id anything
                    :db/ident       :person.gender/female})] :in-any-order))


(fact "emit-dschm - :unique"
  (first (emit-dschm (x/fschm-prepare-ischm {:account {:username [{:unique :identity}]}})))
  => (just
      {:db/id      anything
       :db.install/_attribute :db.part/db,
       :db/ident       :account/username,
       :db/valueType   :db.type/string,
       :db/unique      :db.unique/identity,
       :db/cardinality :db.cardinality/one}))
