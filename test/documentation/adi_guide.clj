(ns documentation.adi-guide
  (:use midje.sweet)
  (:require [adi.core :as adi]))

[[:chapter {:title "Introduction"}]]

"[adi](https://www.github.com/zcaudate/adi) provides a simple, intuitive data layer to access datomic using a document-based syntax, as well as a data-processing pipeline for fine-grain manipulation and access of data."

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/adi \"{{PROJECT.version}}\"]

All functionality is contained in the `adi.core` namespace:
"

(comment
  (require '[adi.core :as adi]))

[[:section {:title "Outline"}]]

"[adi](https://www.github.com/zcaudate/adi) provides the following advantages

- Using the schema as a 'type' system to process incoming data.
- Relations mapped to nested object structures (using a graph-like notion)
- Nested maps/objects as declarative logic queries.
- Custom views on data (similar to the `:pull` api)
"

[[:section {:title "Architecture"}]]

"The architecture can be seen below:"

[[:image {:src "img/adi.png" :width "100%"}]]

[[:chapter {:title "Customisations"}]]

"Who knew that manipulating data can be so terse? Well... it kinda is when you want fine grain control. `adi` comes with a bunch of bells and whistles for data - some inherited from the underlying datomic api, others built to deal with the pipeline for schema-assisted transformation of data."

[[:section {:title "Params"}]]

"There are keywords reserved by the top-level operations that can be set/overwritten through arguments.

Connection related entries:

- `:connection`
- `:db`
- `:at`
- `:return`
- `:transact`

Schema related entries:

- `:schema`
- `:pull`
- `:access`

Pipline related entries:

- `:pipeline`
- `:profiles`
"

[[:section {:title "Options"}]]

"There is a seperate param entry holding all the miscellaneous options, called (as you may have guessed) `options` and each sub-entry will be covered seperately. These can be seperated into particular areas:

Result flags:

- `:first`
- `:ids`

Selection flags:

- `:ban-expressions`
- `:ban-ids`
- `:ban-top-id`
- `:ban-body-ids`

Schema flags:

- `:schema-required`
- `:schema-restrict`
- `:schema-defaults`

Pipeline flags:

- `:use-typecheck`
- `:use-coerce`
- `:skip-normalise`
- `:skip-typesafety`

Debug flags:

- `:simulate`
- `:generate-ids`
- `:generate-syms`
- `:raw`
- `:adi`
"

[[:section {:title "Pipeline"}]]

"The `:pipeline` entry has it's own set of sub-keys. They will be described in a later chapter:

- `:pre-process`
- `:pre-require`
- `:pre-mask`
- `:pre-transform`
- `:fill-empty`
- `:fill-assoc`
- `:ignore`
- `:allow`
- `:validate`
- `:convert`
- `:post-require`
- `:post-mask`
- `:post-transform`
- `:post-process`
"

[[:chapter {:title "Connection Params"}]]

[[:file {:src "test/documentation/adi_guide/reserved/connection.clj"}]]

[[:chapter {:title "Schema Params"}]]

[[:file {:src "test/documentation/adi_guide/reserved/schema.clj"}]]

[[:chapter {:title "Query Options"}]]

[[:file {:src "test/documentation/adi_guide/options/query.clj"}]]

[[:chapter {:title "Normalise Options"}]]

[[:file {:src "test/documentation/adi_guide/options/normalise.clj"}]]

[[:chapter {:title "Debug Options"}]]

[[:file {:src "test/documentation/adi_guide/options/debug.clj"}]]

[[:chapter {:title "Pipeline"}]]

[[:file {:src "test/documentation/adi_guide/reserved/pipeline.clj"}]]
