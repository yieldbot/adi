(ns spirit.datomic.process.normalise.db-test
  (:use hara.test)
  (:require [spirit.common.normalise :as normalise]
            [spirit.datomic.process.normalise.db :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.datomic.process.normalise.db/db-id-syms :added "0.3"}
(fact "creates a compatible db/id symbol"
  (db-id-syms {:id '_}) => {:id '_}
  (db-id-syms {:id 'hello}) => {:id '?hello}
  (db-id-syms {:id 12345}) => {:id 12345})

^{:refer spirit.datomic.process.normalise.db/wrap-db :added "0.3"}
(fact "allows the :db/id key to be used when specifying refs"
  (normalise/normalise {:db/id 'hello
                        :account {:orders {:+ {:db/id '_
                                               :account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-db normalise/wrap-plus]})
  => {:db {:id '?hello}
      :account {:orders {:+ {:db {:id '_} :account {:user "Chris"}}}}})
