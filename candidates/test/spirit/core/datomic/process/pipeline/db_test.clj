(ns spirit.core.datomic.process.pipeline.db-test
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]
            [spirit.core.datomic.process.pipeline.db :refer :all]
            [spirit.data.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.core.datomic.process.pipeline.db/db-id-syms :added "0.3"}
(fact "creates a compatible db/id symbol"
  (db-id-syms {:id '_}) => {:id '_}
  (db-id-syms {:id 'hello}) => {:id '?hello}
  (db-id-syms {:id 12345}) => {:id 12345})

^{:refer spirit.core.datomic.process.pipeline.db/wrap-db :added "0.3"}
(fact "allows the :db/id key to be used when specifying refs"
  (pipeline/normalise {:db/id 'hello
                        :account {:orders {:+ {:db/id '_
                                               :account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-db pipeline/wrap-plus]})
  => {:db {:id '?hello}
      :account {:orders {:+ {:db {:id '_} :account {:user "Chris"}}}}})
