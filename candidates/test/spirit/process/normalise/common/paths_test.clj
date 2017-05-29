(ns spirit.process.normalise.common.paths-test
  (:use hara.test)
  (:require [spirit.process.normalise.base :as normalise]
            [spirit.process.normalise.common.paths :refer :all]
            [spirit.schema :as schema]
            [spirit.test.examples :as examples]
            [spirit.test.checkers :refer [raises-issue]]))

^{:refer spirit.process.normalise.common.paths/wrap-plus :added "0.3"}
(fact "Allows additional attributes (besides the link :ns) to be added to the entity"
  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]})
  => {:account {:orders {:+ {:account {:user "Chris"}}}}}
  ^:hidden
  (normalise/normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)})
  => (throws))

^{:refer spirit.process.normalise.common.paths/wrap-ref-path :added "0.3"}
(fact "Used for tracing the entities through `normalise`"
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-ref-path wrap-plus]})

  => (raises-issue {:ref-path
                    [{:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                     {:account {:WRONG "Chris"}}]}))

^{:refer spirit.process.normalise.common.paths/wrap-key-path :added "0.3"}
(fact "Used for tracing the keys through `normalise`"
  (normalise/normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                       {:schema (schema/schema examples/account-orders-items-image)}
                       {:normalise [wrap-plus]
                        :normalise-branch [wrap-key-path]
                        :normalise-attr [wrap-key-path]})
  =>  (raises-issue {:key-path [:account :orders :+ :account]}))
