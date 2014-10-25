(ns adi.data.common-test
  (:use midje.sweet)
  (:require [adi.data.common :refer :all]))

^{:refer adi.data.common/iid :added "0.3"}
(fact "Constructs a new datomic db/id"
  (iid 1) => #db/id[:db.part/user -1]

  (iid :hello) => #db/id[:db.part/user -245025397])


^{:refer adi.data.common/isym :added "0.3"}
(fact "Returns a new datomic symbol with a unique name. If a prefix string
  is supplied, the name is `prefix#` where `#` is some unique number. If
  prefix is not supplied, the prefix is `e_`."

  (isym) => symbol? ;=> ?e_1238

  (isym "v") => symbol? ;=> ?v1250
  )
