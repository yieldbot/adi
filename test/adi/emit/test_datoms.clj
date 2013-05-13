(ns adi.emit.test-datoms
  (:use midje.sweet
        adi.emit.process
        adi.emit.datoms
        adi.data.samples
        adi.utils))

(facts "datoms helper functions"
  (datoms-data-one {:db {:id 0}
                    :data-one {:account/name "chris"}})
  => [{:db/id 0, :account/name "chris"}]

  (datoms-data-many {:db {:id 0}
                     :data-many {:account/tags #{"t1" "t2" "t3"}}})
  => (just [[:db/add 0 :account/tags "t1"]
            [:db/add 0 :account/tags "t2"]
            [:db/add 0 :account/tags "t3"]] :in-any-order)

  (datoms-refs-one {:db {:id 0}
                    :refs-one {:node/parent {:db {:id 1}}}})
  => [[:db/add 0 :node/parent 1]]

  (datoms-refs-many {:db {:id 0}
                     :refs-many {:node/_parent #{{:db {:id 1}}
                                                 {:db {:id 2}}
                                                 {:db {:id 3}}}}})
  => (just [[:db/add 2 :node/parent 0]
            [:db/add 3 :node/parent 0]
            [:db/add 1 :node/parent 0]]
           :in-any-order))

(facts "datoms"
  (datoms {:db {:id 0}
           :data-one {:node/value "undefined"}
           :refs-many {:node/_parent #{{:db {:id 1}
                                        :data-one {:node/value "child1"}}}}
           :refs-one {:node/parent {:db {:id 2}
                                    :data-one {:node/value "parent1"}}}})
  => (just [{:db/id 2, :node/value "parent1"}
            {:db/id 1, :node/value "child1"}
            {:db/id 0, :node/value "undefined"}
            [:db/add 0 :node/parent 2]
            [:db/add 1 :node/parent 0]]
           :in-any-order))

(facts "emit-datoms-insert"
  (emit-datoms-insert {:account {:name "chris"}} s7-env)
  => (throws Exception)

  (emit-datoms-insert {:db/id 101
                       :account {:id 0 :name "chris"}} s7-env)
  => [{:db/id 101, :account/id 0, :account/name "chris"}]

  (emit-datoms-insert {:account {:id 0 :name "chris"}} s7-env)
  => (just [(just {:db/id anything, :account/name "chris", :account/id 0})])

  (emit-datoms-insert {:node/parent {:children {:value "root"}}} s6-env)
  => (just [(just {:db/id anything, :node/value "root"})
            (just {:db/id anything, :node/value "undefined"})
            (just {:db/id anything, :node/value "undefined"})
            (just [:db/add anything :node/parent anything])
            (just [:db/add anything :node/parent anything])] :in-any-order)

  (emit-datoms-insert {:db/id 0
                       :node/parent {:db/id 1
                                     :children {:db/id 2
                                                :value "root"}}} s6-env)
  => [{:db/id 2, :node/value "root"}
      {:db/id 1, :node/value "undefined"}
      {:db/id 0, :node/value "undefined"}
      [:db/add 2 :node/parent 1]
      [:db/add 0 :node/parent 1]])

(facts "emit-datoms-update"
  (emit-datoms-update {:db/id 101
                       :account {:name "chris"}} s7-env)
  => '({:db/id 101, :account/name "chris"})

  (emit-datoms-update {:db/id 101
                       :account {:id 0 :name "chris" :other :NOT}} s7-env)
  => [{:db/id 101, :account/id 0, :account/name "chris"}]

  (emit-datoms-update {:db/id 0
                       :node/parent {:db/id 1
                                     :children {:db/id 2
                                                :value "root"}}} s6-env)
  => [{:db/id 2, :node/value "root"}
      [:db/add 2 :node/parent 1]
      [:db/add 0 :node/parent 1]])



(def l1-env
  (process-init-env {:link {:value  [{:fulltext true}]
                            :next [{:type :ref
                                    :ref  {:ns :link
                                           :rval :prev}}]
                            :node [{:type :ref
                                    :ref {:ns :node}}]}
                     :node {:value  [{}]
                            :parent [{:type :ref
                                        :ref  {:ns :node
                                               :rval :children}}]}}))

(def l1-fgeni (-> l1-env :schema :fgeni))

(def l1-data
  {:db/id (iid :start)
   :link {:value "l1"
          :next {:value "l2"
                 :next {:value "l3"
                        :next {:+ {:db/id (iid :start)}}}}}})

(emit-datoms-insert l1-data l1-env)
