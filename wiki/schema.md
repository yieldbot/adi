# geni

The geni is a shorthand version of a datomic schema. This is a short-hand of http://docs.datomic.com/schema.html

### Basics

Lets show off some things we can do with adi. We start off by defining a user account and gradually add more features as we go along. 

Fire up nrepl/emacs/vim and load `adi.core`.
```clojure
(require '[adi.core :as adi])
```
We first have to define a `geni`, which is a template for our data:
```clojure            
(def geni-1
 {:account/user     [{:type :string      ;; (1)
                      :cardinality :one  
                      :unique :value     ;; (2)
                      :required true}]   ;; (3)
  :account/password [{:required true     ;; (1) (3)
                      :restrict ["password needs an integer to be in the string" ;; (4)
                                   #(re-find #"\d" %)]}]}})
```
There are a couple of things to note about our definitions the entry.
  1. We specified the `:type` for `:account/user` to be `:string` and `:cardinality` to be `:one`. However, because these are default options, we can optionally leave them out for `:account/password`.
  2. We want the value of `:account/user` to be unique.
  3. We require that both `:account/user` and `:account/password` to be present on insertion.
  4. We are checking that `:account/password` contains at least one number

Now, we construct a datastore. 
```clojure            
(def ds (adi/datastore "datomic:mem://example-1" geni-1 true true))
```
The parameters are:
   uri   The uri of the datomic database to connect to
   geni  The previously defined data template 
   install?  - Optional flag (if true, will install the `geni` into the database) 
   recreate? - Optional flag (if true will delete and then create the database)

So lets attempt to add some data:
```clojure
(adi/insert! ds {:account {:user "adi"}})
;;=> (throws Exception "The following keys are required: #{:account/password}")

(adi/insert! ds {:account {:user "adi" :password "hello"}})
;;=> (throws Exception "The value hello does not meet the restriction: password needs an integer to be in the string")

(adi/insert! ds {:account {:user "adi" :password "hello1" :type :vip}})
;;=> (throws Exception "(:type :vip) not in schema definition")

(adi/insert! ds {:account {:user "adi" :password "hello1"}})
;;=> Yay!!!
(adi/select ds :account)
```

### Reference
- type  
specifies the type of value that can be associated with an attribute. The type is expressed as a keyword. 
(:type defaults to :string). Allowable values are listed below:

   :keyword 
   Value type for keywords. Keywords are used as names, and are interned for efficiency. Keywords map to the native interned-name type in languages that support them.
   :string - Value type for strings.

   :boolean - Boolean value type.

   :long - Fixed integer value type. Same semantics as a Java long: 64 bits wide, two's complement binary representation.

   :bigint - Value type for arbitrary precision integers. Maps to java.math.BigInteger on Java platforms.

   :float - Floating point value type. Same semantics as a Java float: single-precision 32-bit IEEE 754 floating point.

   :double - Floating point value type. Same semantics as a Java double: double-precision 64-bit IEEE 754 floating point.

   :bigdec - Value type for arbitrary precision floating point numbers. Maps to java.math.BigDecimal on Java platforms.

   :ref - Value type for references. All references from one entity to another are through attributes with this value type.

   :instant - Value type for instants in time. Stored internally as a number of milliseconds since midnight, January 1, 1970 UTC. Maps to java.util.Date on Java platforms.

   :uuid - Value type for UUIDs. Maps to java.util.UUID on Java platforms.

   :uri - Value type for URIs. Maps to java.net.URI on Java platforms.

   :bytes - Value type for small binary data. Maps to byte array on Java platforms.
   
   :enum (adi only) - Value type for enums.

- cardinality
  (:cardinality defaults to :one)
  specifies whether an attribute associates a single value or a set of values with an entity. The values allowed for :db/cardinality are:
   :one - the attribute is single valued, it associates a single value with an entity
   :many - the attribute is multi valued, it associates a set of values with an entity
   
- unique  
  (defaults to nil)
  specifies a uniqueness constraint for the values of an attribute. The values allowed for :db/unique are
  :value - the attribute value is unique to each entity; attempts to insert a duplicate value for a different entity id will fail
  :identity - the attribute value is unique to each entity and "upsert" is enabled; attempts to insert a duplicate value for a temporary entity id will cause all attributes associated with that temporary id to be merged with the entity already in the database.
- doc  
  specifies a documentation string. <string>
- index <boolean>
   specifies a boolean value indicating that an index should be generated for this attribute. Defaults to false.
- fulltext <boolean>
   specifies a boolean value indicating that a fulltext search index should be generated for the attribute. Defaults to false.
- isComponent <boolean>
   specifies that an attribute whose type is :db.type/ref refers to a subcomponent of the entity to which the attribute is applied. When you retract an entity with :db.fn/retractEntity, all subcomponents are also retracted. When you touch an entity, all its subcomponent entities are touched recursively. Defaults to nil.
- noHistory <boolean>
   specifies a boolean value indicating whether past values of an attribute should not be retained. Defaults to false. <boolean>
     
additional adi options

- required <boolean>
   specifies whether the attribute is a required one. This will only take effect on insertion of data.
   
- default <type value> or <function>
   specifies the default value of the attribute. This will only take effect on insertion of data.
    
- restrict <function> or <set>
   specifies a refined restriction on what data can be added. This will only take effect on inserts and updates
   
- ref
  hashmap with the following sub attributes:
  - ns <keyword>
    specifies a the namespace of the ref attributes
    
  - rval <keyword>
    specifies a reverse lookup value
    
  - norev <boolean>
    specifies that reverse lookups should not be installed
 
- keyword
  - ns <keyword>
    specifies the namespace of the keyword

- enum
  - ns <keyword>
    specifies the namesace of the enum
    

### required

### default

### restrict

