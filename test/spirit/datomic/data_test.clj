(ns spirit.datomic.data-test
  (:use hara.test)
  (:require [spirit.datomic.data :refer :all]))

^{:refer spirit.datomic.data/iid :added "0.3"}
(fact "Constructs a new datomic db/id"
  (iid 1) => #db/id[:db.part/user -1]

  (iid :hello) => #db/id[:db.part/user -245025397])


^{:refer spirit.datomic.data/isym :added "0.3"}
(fact "Returns a new datomic symbol with a unique name. If a prefix string
  is supplied, the name is `prefix#` where `#` is some unique number. If
  prefix is not supplied, the prefix is `e_`."

  (isym) => symbol? ;=> ?e_1238

  (isym "v") => symbol? ;=> ?v1250
  )

^{:refer spirit.datomic.data/vexpr->expr :added "0.3"}
(fact "checks whether an input is a vector expression"
  (vexpr->expr [["_"]]) => '_

  (vexpr->expr [["?hello"]]) => '?hello

  (vexpr->expr [["(< ? 1)"]]) => '(< ? 1)

  (vexpr->expr [[":hello"]]) => #db/id[:db.part/user -245025397])
