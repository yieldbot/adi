(ns documentation.spirit-core-datomic.schema
  (:use hara.test)
  (:require [spirit.datomic :as datomic]))

[[:section {:title "Basics"}]]

"The schema can be defined as follows:"

(comment
  (def schema
    {:<ns> {:<name0> [{<attr0> <value>
                      <attr1> <value>}]
            :<name1> {:<sub> [{<attr0> <value>
                              <attr1> <value>}]}}
     :<ns>/<name>.<sub> [{<attr0> <value>
                          <attr1> <value>}]}))

"Where the concatenated `<ns>`, `<name>` and `<sub>` maps to datomic's `:db/ident` and a map inside a vector represents definition of database attributes."

[[:subsection {:title "Shorthand"}]]

"`spirit` offers sane defaults and attempts to remove noise as much as possible. Therefore, the datomic schema:"

(comment
  {:db/id #db/id[:db.part/db]
   :db/ident :person/name
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one
   :db/doc "A person's name"
   :db.install/_attribute :db.part/db})

"Can be represented in spirit as:"

(comment
  {:person {:name [{:doc "a person's name"}]}})

"And if the defaults were made explicit:"

(comment
  {:person {:name [{:type :string
                    :cardinality :one
                    :doc "a person's name"}]}})

[[:section {:title "Attributes"}]]

"Core:

- `:type`
- `:ref`
- `:enum`
- `:cardinality`

Optionals:

- `:unique`
- `:doc`
- `:index`
- `:fulltext`
- `:isComponent`
- `:noHistory`

Additional

- `:required`
- `:restrict`
- `:default`
"

[[:section {:title ":type"}]]

"Specifies the type of value that can be associated with an attribute. The type is expressed as a keyword. The values allowed are:

- `:bigdec` arbitrary precision floating point numbers, maps to `java.math.BigDecimal`
- `:bigint` arbitrary precision integers, maps to `java.math.BigInteger`.
- `:boolean` boolean data.
- `:bytes`  small binary data. Maps to byte array.
- `:double` double-precision 64-bit IEEE 754 floating point.
- `:enum` typed keywords.
- `:float` single-precision 32-bit IEEE 754 floating point.
- `:instant` instants in time, maps to `java.util.Date`.
- `:keyword` keyword data.
- `:long` 64 bits wide, two's complement binary
- `:ref` reference to another entity
- `:string` (default) string data
- `:uri` maps to `java.net.URI`
- `:uuid` maps to `java.util.UUID`
"

[[:section {:title ":ref"}]]

"If the `:type` is of `:ref`, the additional customisations are required with the `:ref` attribute. The treatment of `:ref`s as well as a more natural reverse search syntax allows more control over the original datomic syntax.

- `:ns` main namespace of the reference to the entity
- `:rval` the name of the back reference"

"An example can be seen below where account has references to books and books also has references to account by pluralizing:"

(comment
  (datomic/schema {:account {:book [{:type :ref
                                 :ref {:ns :book}}]}
                   :book {:name [{}]}})
  ;;=> #schema{:account {:book :&book}, :book {:name :string, :accounts :&account<*>}}
  )

"The reverse reference can be explicitly be set via `:rval`:"

(comment
  (datomic/schema {:account {:book [{:type :ref
                                 :ref {:ns :book
                                       :rval :users}}]}
               :book {:name [{}]}})
  ;;=> #schema{:account {:book :&book}, :book {:name :string, :users :&account<*>}}
  )

"The reference can refer to it's own namespace:"

(comment
  (datomic/schema {:node/next [{:type :ref
                            :ref {:ns :node
                                  :rval :previous}}]})
  ;;=> #schema{:node {:next :&node, :previous :&node<*>}}
  )

"Reference to it's own without an `:rval` adds `_of` when generating the reference reference:"

(comment
  (datomic/schema {:node/link   [{:type :ref
                              :ref {:ns :node}}]
               :node/parent [{:type :ref
                              :ref {:ns :node}}]})
  ;;=> #schema{:node {:link :&node
  ;;           :parent :&node
  ;;           :link_of :&node<*>
  ;;           :parent_of :&node<*>}}
  )

"Multiple references to the same namespace can also be differentiated automatically:"

(comment
  (datomic/schema {:account {:like [{:type :ref
                                 :ref {:ns :book}}]
                         :dislike [{:type :ref
                                 :ref {:ns :book}}]}
               :book {:name [{}]}})
  ;;=> #schema{:account {:like :&book, :dislike :&book},
  ;;           :book {:name :string,
  ;;                  :like_accounts :&account<*>,
  ;;                  :dislike_accounts :&account<*>}}
  )

[[:section {:title ":enum"}]]

"If the `:type` is of `:enum`, then additional customisations are required with the `:enum` attribute:

- `:ns` namespace for enum values
- `:values` possible values for enum"

(comment
  {:account/type     [{:type :enum
                       :enum {:ns :account.type
                              :values #{:admin :free :paid}}}]})

"Internally, the data types stored are refs to entities with ids: `:account.type/admin`, `:account.type/free` and `:account.type/paid`."

[[:section {:title ":cardinality"}]]

"Specifies whether an attribute associates a single value or a set of values with an entity. The values allowed are:

- `:one` (default) associates a single value with an entity
- `:many` associates a set of values with an entity"

[[:section {:title ":unique"}]]

"Specifies a uniqueness constraint for the values of an attribute. Setting an attribute `:unique` also implies `:index`. The values allowed for are:

- `:value` only one entity can have a given value for this attribute
- `:identity` attempts to insert a duplicate value for a temporary entity id will cause all attributes associated with that temporary id to be merged with the entity
- `nil` (default)
"

[[:section {:title ":doc"}]]

"Specifies a documentation string for the attribute"

(comment
  (datomic/schema {:node/link   [{:type :ref
                              :doc "The link to the next node"
                              :ref {:ns :node}}]})
  ;;=> #schema{:node {:link :&node, :link_of :&node<*>}}
  )

[[:section {:title ":index"}]]

"Specifies a boolean value indicating that an index should be generated for this attribute. Defaults to `false`"

[[:section {:title ":fulltext"}]]

"Specifies a boolean value indicating that an eventually consistent fulltext search index should be generated for the attribute. Defaults to `false`"

[[:section {:title ":isComponent"}]]

"Specifies a boolean value indicating that an attribute whose type is `:ref` refers to a subcomponent of the entity to which the attribute is applied. When you retract an all subcomponents are also retracted. When you touch an entity, all its subcomponent entities are touched recursively. Defaults to false."

[[:section {:title ":noHistory"}]]

"Specifies a boolean value indicating whether past values of an attribute should not be retained."


[[:section {:title ":required"}]]

"Specifies a boolean value indicating that that when data is inserted, that the entry exists. Does not matter for update and query. See [walkthrough](./spirit-walkthrough.html#step-one) for example."

[[:section {:title ":restrict"}]]

"Specifies a vector, the first being a readable message, the second being the predicate. Like `:required`, only works for data insertion. See [walkthrough](./spirit-walkthrough.html#step-one) for example."

[[:section {:title ":default"}]]

"Specifies a value that will be inserted if nothing is specified. See [walkthrough](./spirit-walkthrough.html#step-one) for example."
