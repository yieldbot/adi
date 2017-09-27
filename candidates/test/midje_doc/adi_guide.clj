(ns midje-doc.adi-guide
  (:use midje.sweet)
  (:require [datomic.api :as d]))

[[:chapter {:title "Interface"}]]

"
The problem of security is actually an interface problem. At the heart of every system lies the database. It is exceedingly powerful, and hazardous to wield directly. A wrong keystroke could potentially wipe out the company's entire bank of clients, recievables and other such intellectual property. Anyone with direct access can do a lot of damage and so it much be protected (just as we also protect our internal organs).

If we view an application as a cell, then the database can be considered the nucleus and the rest of the application is like a porous membrane and a network of internal receptors/transmittors through which information can be directed in and out. This network is defined by the application developer as a structure of checks, restrictions and transformations. Ultimately, both systems provide a pipeline to go through such that the database only exposes the right data to the right authority.

The irony for the developer is that data from the outside world comes in all shapes and sizes. The more controls are defined, the less power and control one has over the data. The current web-development is almost entirely devoted to security and data transformation -> from json, to objects/maps, to validating them and converting them to records and then back again.

Relational databases rely on orms for converting from data to tables. Document databases allow data to be stored as a tree structure at the expense of query power. However, none of the above address the issue of security. Much of current webapp code is code to deal with security. 

Adi was designed to address these issues, consisting of a declarative security model, a compact object-based syntax whilst maintaining the power of query.

"

;;[[:file {:src "test/midje_doc/adi_dev/schema.clj"}]]
[[:file {:src "test/midje_doc/adi_dev/operators.clj"}]]