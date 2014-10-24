(ns adi.normalise.common.paths-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.paths :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

^{:refer adi.normalise.common.paths/wrap-plus :added "0.3"}
(fact "Allows additional attributes (besides the link :ns) to be added to the entity"
  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]})
  => {:account {:orders {:+ {:account {:user "Chris"}}}}}
  ^:hidden
  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)})
  => (throws))

^{:refer adi.normalise.common.paths/wrap-ref-path :added "0.3"}
(fact "Used for tracing the entities through `normalise`"
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-ref-path wrap-plus]})

  => (raises-issue {:ref-path
                    [{:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                     {:account {:WRONG "Chris"}}]}))

^{:refer adi.normalise.common.paths/wrap-key-path :added "0.3"}
(fact "Used for tracing the keys through `normalise`"
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]
                        :normalise-branch [wrap-key-path]
                        :normalise-attr [wrap-key-path]})
  =>  (raises-issue {:key-path [:account :orders :+ :account]}))
