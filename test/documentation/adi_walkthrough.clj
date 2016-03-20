(ns documentation.adi-walkthrough)

[[:chapter {:title "Introduction"}]]

[[:section {:title "Installation"}]]

"
Add to `project.clj` dependencies:

    [im.chit/adi \"{{PROJECT.version}}\"]

All functionality is contained in the `adi.core` namespace:
"

(comment
  (require '[adi.core :as adi]))


[[:section {:title "Motivation"}]]
"
At the heart of any modern system lies the database. The database has evolved to become exceedingly powerful with facilities for data management, clustering, concurrent access and search. However, the database is also too powerful to expose directly to the outside world. A malicous attacker, or more likely, a wrong keystroke could wipe out an application's entire list of clients, recievables and other such intellectual property. Anyone with direct access can do a lot of damage and so it much be protected.

If we view an application as a cell, then the database can be considered the nucleus and the rest of the application like a porous membrane and a network of internal receptors/transmittors through which information can be directed in and out. This network is defined by the application developer as a structure of checks, restrictions and transformations. Ultimately, any robust system has to provide a pipeline for such checks so that the database only exposes the right data to the right authority as well as recieves data that has been deemed safe to enter.

Security is then the specification of an interface for access to the database. The irony for the developer is that data from the outside world comes in all shapes and sizes. The more controls are defined, the less power and control one has over the data. The current web-development is almost entirely devoted to security and data transformation -> from json, to objects/maps, to validating them and converting them to records and then back again.

Relational databases rely on orms for converting from data to tables. Document databases allow data to be stored as a tree structure at the expense of query power. However, none of the above address the issue of security. Much of current code for web applications is code to deal with security.

[adi](https://www.github.com/zcaudate/adi) has been designed to generalize the mechanisms for allowing data from an insecure web interface directly into the database. It consists of a declarative security model, a compact object-based syntax that is built on top of datomic.
"

[[:section {:title "Off and Running"}]]

"Open up a repl in a project with `adi` installed and require load up the library:"


[[:file {:src "test/documentation/adi_walkthrough/step_1.clj"}]]
[[:file {:src "test/documentation/adi_walkthrough/step_2.clj"}]]
[[:file {:src "test/documentation/adi_walkthrough/step_3.clj"}]]
[[:file {:src "test/documentation/adi_walkthrough/bookstore.clj"}]]
[[:file {:src "test/documentation/adi_walkthrough/schoolyard.clj"}]]
