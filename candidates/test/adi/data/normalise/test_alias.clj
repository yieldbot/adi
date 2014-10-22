(ns adi.data.normalise.test-alias
  (:use midje.sweet)
  (:require
   [adi-test.schemas :refer :all]
   [adi-test.checkers :refer :all]
   [hara.collection.hash-map :refer [treeify-keys]]
   [adi.model :refer [model-input]]
   [adi.data.normalise :as t :refer [normalise]]))

(fact
  (normalise {:db/id 'chris
              :person/name "Chris"}
             {:schema family-links-xm})
  => {:db {:id '?chris} :person {:name "Chris"}}

  (normalise {:db/id 'chris
              :male/name "Chris"}
             {:schema family-links-xm})
  => {:db {:id '?chris} :person {:name "Chris" :gender :m}}

  (normalise {:female {:parent/name "Sam"
                       :brother/brother/name "Chris"}}
             {:schema family-links-xm})
  => {:person {:gender :f, :parent #{{:name "Sam"}},
               :sibling #{{:gender :m, :sibling #{{:name "Chris", :gender :m}}}}}}

  (normalise {:female/uncle/name "Sam"}
             {:schema family-links-xm})
  => {:person {:gender :f, :parent #{{:sibling #{{:gender :m} {:name "Sam"}}}}}}

  (normalise {:female/granddaughter/name "Sam"}
             {:schema family-links-xm})
  => {:person {:gender :f, :child #{{:child #{{:gender :f} {:name "Sam"}}}}}}

  (normalise {:female/niece/name "Sam"}
             {:schema family-links-xm})
  => {:person {:gender :f, :sibling #{{:child #{{:gender :m, :name "Sam"}}}}}}

  (normalise {:female/full-sibling/name "Sam"}
             {:schema family-links-xm
              :options {:no-alias-gen true}})
  => '{:person {:gender :f,
                :parent #{{:+ {:db {:id ?y}}, :gender :f}
                          {:+ {:db {:id ?x}}, :gender :m}},
                :sibling #{{:name "Sam",
                            :parent #{{:+ {:db {:id ?y}}, :gender :f}
                                      {:+ {:db {:id ?x}}, :gender :m}}}}}}

   (normalise {:female/elder-brother/name "Sam"}
              {:schema family-links-xm
               :options {:no-alias-gen true}
               :type "query"})
   => '{:person {:gender #{:f},
                :age #{?x},
                :sibling #{{:name #{"Sam"},
                            :gender #{:m},
                            :age #{(< ?x)}}}}})
