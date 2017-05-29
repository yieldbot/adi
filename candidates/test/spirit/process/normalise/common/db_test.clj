(ns spirit.process.normalise.common.db-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.db :refer :all]
            [spirit.process.normalise.common.paths :as paths]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.db/db-id-syms :added "0.3"}
(fact "creates a compatible db/id symbol"
  (db-id-syms {:id '_}) => {:id '_}
  (db-id-syms {:id 'hello}) => {:id '?hello}
  (db-id-syms {:id 12345}) => {:id 12345})

^{:refer spirit.process.normalise.common.db/wrap-db :added "0.3"}
(fact "allows the :db/id key to be used when specifying refs"
  (normalise/normalise {:db/id 'hello
                        :account {:orders {:+ {:db/id '_
                                               :account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-db paths/wrap-plus]})
  => {:db {:id '?hello}
      :account {:orders {:+ {:db {:id '_} :account {:user "Chris"}}}}})
