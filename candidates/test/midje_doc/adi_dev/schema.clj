(ns midje-doc.spirit-dev.schema
  (:use hara.test
        spirit.schema.types
        spirit.schema.xm
        spirit.schema.emit))

[[:chapter {:title "Schema"}]]


[[:section {:title "Terminology"}]]


"The word **schema** is a very general term. In the context of `spirit` we have to define the following types of schemas:

 - `schema` or `ischm`
 - `flat-schema` or `fschm`
 - `tree-schema` or `tschm`
 - `executable-schema` or `xm`
 - `datomic-schema` or `dschm`
 - `meta-schema` or `mschm`"

[[:subsection {:title "input-schema / ischm"}]]

"The term `input-schema` or ischm is the term given to a datastructure that the user/programmer has specified."

[[:subsection {:title "meta-schema / mschm"}]]

"The term `meta-schema` or mschm contains all information used to describe spirit schemas."

[[:subsection {:title "flat-schema / fschm"}]]

"The term `flat-schema` or fschm is used to describe a hashmap with no nesting"

[[:subsection {:title "tree-schema / tschm"}]]

"The term `tree-schema` or tschm is used to describe a hashmap with full nesting"

[[:subsection {:title "executable-schema / xm"}]]

"The term `executable-schema` or xm is used to describe a hashmap with both a tree, and a flat schema as well as a lookup. This is the main workhorse for `spirit`."

[[:subsection {:title "datomic-schema / dschm"}]]

"The term `datomic-schema` or dschm is used to describe a hashmap with both a tree, and a flat schema as well as a lookup."


[[:section {:title "The Goal"}]]

"The Goal is to transform an input schema into an xm and then to transform the xm into a raw datomic schema."

(fact
  (def xm (make-xm {:account/email [{}]}))

  (keys xm)
  => '(:tree :flat :lu)

  (:tree xm)
  => {:account {:email [{:ident :account/email
                         :type :string
                         :cardinality :one}]}}
  (:flat xm)
  => {:account/email [{:ident :account/email
                       :type :string
                       :cardinality :one}]}

  (:lu xm)
  => {:account/email :account/email})


(fact
  (emit-dschm (:flat xm))
  => (just
      (just
       {:db/id db-id?
        :db.install/_attribute :db.part/db
        :db/ident :account/email
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one})))
