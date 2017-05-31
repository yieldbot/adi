(ns spirit.datomic.process.normalise.pipeline.expression-test
  (:use hara.test)
  (:require [spirit.datomic.process.normalise.base :as normalise]
            [spirit.datomic.process.normalise.common.paths :as paths]
            [spirit.datomic.process.normalise.common.list :as list]
            [spirit.datomic.process.normalise.pipeline.expression :as expression]
            [spirit.common.schema :as schema]
            [data.examples :as examples]
            ))

(def ^:dynamic *wrappers*
  {:normalise        [paths/wrap-plus]
   :normalise-single [list/wrap-single-list]
   :normalise-expression [expression/wrap-single-model-expression]
   :normalise-branch [paths/wrap-key-path]
   :normalise-attr   [paths/wrap-key-path]})
   
^{:refer spirit.datomic.process.normalise.pipeline.expression/wrap-single-model-expression :added "0.3"}
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
   => (throws-info {:failed-check true})

   (normalise/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {}}}
                *wrappers*)
   => (throws-info {:failed-check true}))
