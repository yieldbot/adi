(ns documentation.adi-guide
  (:use midje.sweet)
  (:require [adi.core :as adi]))

[[:chapter {:title "Introduction"}]]

[[:section {:title "Installation"}]]

[[:section {:title "Motivation"}]]


[[:chapter {:title "Connection Params"}]]

"There are keywords reserved by the top-level operations that can be set/overwritten through arguments. They can be roughly split into `:connection` related entries and `:schema` related entries. The connection related entries are listed as follows:

- `:connection`
- `:db`
- `:at`
- `:return`
- `:transact`

And the schema related entries are listed as follows:

- `:schema`
- `:pull`
- `:access`

Furthermore, there is a 

- `:pipeline`
- `:profiles`


There is a seperate entry holding all the miscellaneous options, called (as you may have guessed) `options` and each sub-entry will be covered seperately in a later chapter.
"

[[:file {:src "test/documentation/adi_guide/reserved/connection.clj"}]]

[[:chapter {:title "Schema Params"}]]

[[:file {:src "test/documentation/adi_guide/reserved/schema.clj"}]]

[[:file {:src "test/documentation/adi_guide/reserved/model.clj"}]]


[[:chapter {:title "Model"}]]

[[:chapter {:title "Options"}]]

"Options are seperated into particular areas:

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

They will each be presented in their own seperate sections."

[[:file {:src "test/documentation/adi_guide/options.clj"}]]
