(ns midje-doc.adi-guide)

[[:chapter {:title "Introduction"}]]

"### Installation

Add to `project.clj` dependencies

`[im.chit/adi `\"`{{PROJECT.version}}`\"`]`.

All the functionality can be loaded using:"

(comment
  (require '[adi.core :as adi]))


"### Overview

`adi` is not for the faint of heart. It has been built especially for the creative, crazy and super ambitious. `adi` is a modelling and data-access language for datomic, allowing one to create rich applications through schema driven expression. `adi` especially was designed for modelling complexity - little apps just don't cut it - the bigger, more intertwined the data model becomes, the more `adi` will have its chance to shine.

So what exactly is `adi`?

- It empowers `datomic` to something best described as a `\"document database on steroids\"`.
- It functions as a `schema-driven`, `document` and `graph` database hybrid.
- It simplifies `crud` operations to be `logical`, `declarative` and `data-centric`.
- It uses the `schema` and an application `pipeline` to process incoming data, similar to that of what a `type system`.
- It is `simple` and `expressive`, a pure `joy` to use.

The concept is simple. `adi` is a document-database syntax grafted on `datomic`. It makes use of a map/object notation to generate datastructure for the `datomic` query engine. This provides for an even more declarative syntax for relational search. Fundamentally, there should be no difference in the data-structure between what the programmer uses to ask and what the programmer is getting back. We shouldn't have to play around turning objects into records, objects into queries... etc...

*Not Anymore.*

`adi` converts flat, record-like arrays to tree-like objects and back again. The key to understanding adi lies in understanding the power of a schema. The schema dictates what you can do with the data. Instead of limiting the programmer, the schema should exhance him/her, much like what a type-system does for programmers - without being suffocatingly restrictive. Once a schema for an application has been defined, the data can be inserted in ANY shape or form, as long as it follows the conventions specified within that schema.
"

[[:chapter {:title "Tutorial"}]]

[[:file {:src "test/adi/examples/step_1.clj"}]]
[[:file {:src "test/adi/examples/step_2.clj"}]]
[[:file {:src "test/adi/examples/step_3.clj"}]]

[[:file {:src "test/adi/examples/step_4.clj"}]]
