(ns spirit.datomic.process.pipeline.expression-test
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]
            [spirit.datomic.process.pipeline.list :as list]
            [spirit.datomic.process.pipeline.expression :as expression]
            [spirit.data.schema :as schema]
            [data.examples :as examples]))

(def ^:dynamic *wrappers*
  {:normalise        [pipeline/wrap-plus]
   :normalise-single [list/wrap-single-list]
   :normalise-expression [expression/wrap-single-model-expression]
   :normalise-branch [pipeline/wrap-key-path]
   :normalise-attr   [pipeline/wrap-key-path]})
   
^{:refer spirit.datomic.process.pipeline.expression/wrap-single-model-expression :added "0.3"}
(fact "controls the expressions allowed for searches"
   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '(= "Chris")}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '#{=}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}
   ^:hidden
   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name #{'(= _)}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name #{'(= string?)}}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '_}}}}
                *wrappers*)
   => {:account {:name '(= "Chris")}}

   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {:account {:name '#{not=}}}}}
                *wrappers*)
   => (throws-info {:failed-check true})

   (pipeline/normalise {:account/name '(= "Chris")}
                {:schema (schema/schema examples/account-name-age-sex)
                 :pipeline {:expression {}}}
                *wrappers*)
   => (throws-info {:failed-check true}))
