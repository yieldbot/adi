

The geni is a shorthand version of a datomic schema. This is a short-hand of http://docs.datomic.com/schema.html

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

