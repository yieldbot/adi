(ns spirit.common.pipeline-test
  (:use hara.test)
  (:require [spirit.common.pipeline :refer :all]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

^{:refer spirit.common.pipeline/submaps :added "0.3"}
(fact "creates a submap based upon a lookup subkey"
  (submaps {:allow  {:account :check}
            :ignore {:account :check}} #{:allow :ignore} :account)
  => {:allow :check, :ignore :check})

^{:refer spirit.common.pipeline/normalise :added "0.3"}
(fact "base normalise function for testing purposes"

  (normalise {:account/name "Chris"
              :account/age 10}
             {:schema (schema/schema examples/account-name-age-sex)}
             {})
  => {:account {:age 10, :name "Chris"}}

  (normalise {:link/value "hello"
              :link {:next/value "world"
                     :next/next {:value "!"}}}
             {:schema (schema/schema examples/link-value-next)}
             {})
  => {:link {:next {:next {:value "!"}
                    :value "world"}
             :value "hello"}})


^{:refer spirit.common.pipeline/wrap-plus :added "0.3"}
(fact "Allows additional attributes (besides the link :ns) to be added to the entity"
  (normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
             {:schema (schema/schema examples/account-orders-items-image)}
             {:normalise [wrap-plus]})
  => {:account {:orders {:+ {:account {:user "Chris"}}}}}
  ^:hidden
  (normalise {:account {:orders {:+ {:account {:user "Chris"}}}}}
             {:schema (schema/schema examples/account-orders-items-image)}
             {})
  => (throws))

^{:refer spirit.common.pipeline/wrap-ref-path :added "0.3"}
(fact "Used for tracing the entities through `normalise`"
  (normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
             {:schema (schema/schema examples/account-orders-items-image)}
             {:normalise [wrap-ref-path wrap-plus]})

  => (throws-info {:ref-path
                    [{:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
                     {:account {:WRONG "Chris"}}]}))

^{:refer spirit.common.pipeline/wrap-key-path :added "0.3"}
(fact "Used for tracing the keys through `normalise`"
  (normalise {:account {:orders {:+ {:account {:WRONG "Chris"}}}}}
             {:schema (schema/schema examples/account-orders-items-image)}
             {:normalise [wrap-plus]
              :normalise-branch [wrap-key-path]
              :normalise-attr [wrap-key-path]})
  =>  (throws-info {:key-path [:account :orders :+ :account]}))
