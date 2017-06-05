(ns spirit.datomic.process.normalise.expression-test
  (:use hara.test)
  (:require [spirit.common.normalise :as normalise]
            [spirit.datomic.process.normalise.list :as list]
            [spirit.datomic.process.normalise.expression :as expression]
            [spirit.common.schema :as schema]
            [data.examples :as examples]))

(def ^:dynamic *wrappers*
  {:normalise        [normalise/wrap-plus]
   :normalise-single [list/wrap-single-list]
   :normalise-expression [expression/wrap-single-model-expression]
   :normalise-branch [normalise/wrap-key-path]
   :normalise-attr   [normalise/wrap-key-path]})
   
^{:refer spirit.datomic.process.normalise.expression/wrap-single-model-expression :added "0.3"}
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
