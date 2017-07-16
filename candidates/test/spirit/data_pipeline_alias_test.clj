(ns spirit.data.pipeline.base.alias-test
  (:use hara.test)
  (:require [spirit.data.pipeline :as pipeline]
            [spirit.data.pipeline.base.alias :refer :all]
            [spirit.core.datomic.process.pipeline.db :as db]
            [spirit.core.datomic.process.pipeline.set :as set]
            [spirit.data.schema :as schema]
            [data.family :as family]))

(def ^:dynamic *wrappers*
  {:normalise [db/wrap-db pipeline/wrap-plus wrap-alias]
   :normalise-attr [set/wrap-attr-set]
   :normalise-branch [wrap-alias]})

^{:refer spirit.data.pipeline.base.alias/wrap-alias :added "0.3"}
(fact "wraps normalise to process aliases for a database schema"

  (pipeline/normalise {:db/id 'chris
                        :male/name "Chris"}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => '{:db {:id ?chris}, :person {:gender :m, :name "Chris"}}

  (pipeline/normalise {:female {:parent/name "Sam"
                                 :brother {:brother/name "Chris"}}}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => {:person {:gender :f, :parent #{{:name "Sam"}},
               :sibling #{{:gender :m, :sibling #{{:name "Chris", :gender :m}}}}}}
  ^:hidden
  (pipeline/normalise {:female {:granddaughter/name "Sam"}}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => {:person {:gender :f, :child #{{:child #{{:gender :f} {:name "Sam"}}}}}}

  (pipeline/normalise {:female/uncle {:name "Sam"}}
                       {:schema (schema/schema family/family-links)}
                       *wrappers*)
  => {:person {:gender :f, :parent #{{:sibling #{{:gender :m} {:name "Sam"}}}}}}

  (pipeline/normalise {:male {:full-sibling/name "Sam"}}
                       {:schema (schema/schema family/family-links)
                        :options {:no-alias-gen true}}
                       *wrappers*)
  => {:person {:gender :m, :sibling #{{:name "Sam", :parent #{{:+ {:db {:id '?x}}, :gender :m}
                                                               {:+ {:db {:id '?y}}, :gender :f}}}},
                :parent #{{:+ {:db {:id '?x}}, :gender :m} {:+ {:db {:id '?y}}, :gender :f}}}})
