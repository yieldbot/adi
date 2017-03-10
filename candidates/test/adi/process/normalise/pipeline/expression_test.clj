(ns adi.process.normalise.pipeline.expression-test
  (:use midje.sweet)
  (:require [adi.process.normalise.base :as normalise]
            [adi.process.normalise.common.paths :as paths]
            [adi.process.normalise.common.list :as list]
            [adi.process.normalise.pipeline.expression :as expression]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [list/wrap-single-list]
   :normalise-expression [expression/wrap-single-model-expression]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})
   
^{:refer adi.process.normalise.pipeline.expression/wrap-single-model-expression :added "0.3"}
(fact "controls the expressions allowed for searches"
   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '(= "Chris")}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '#{=}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}
   ^:hidden
   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name #{'(= _)}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name #{'(= string?)}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '_}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '#{not=}}}}}
                *wrappers*)
   => (raises-issue {:failed-check true})

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {}}}
                *wrappers*)
   => (raises-issue {:failed-check true}))
