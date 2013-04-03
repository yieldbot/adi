(ns adi.test-deprocess
  (:use midje.sweet
        adi.utils
        adi.schema
        adi.data
        adi.checkers))

(def d1-env
  (process-init-env {:node {:value  [{:default "undefined"}]
                            :parent [{:type :ref
                                      :ref  {:ns :node
                                             :rval :children}}]}
                     :leaf {:value [{:default "leafy"}]
                          :node  [{:type :ref
                                   :ref {:ns :node
                                         :rval :leaves}}]}}))

(def d1-fgeni (-> d1-env :schema :fgeni))


(fact "deprocess-view-key"
  (deprocess-view-key #{} :node/value)
  => nil

  (deprocess-view-key #{:node/value} :node/value)
  => :show

  (deprocess-view-key {:node/value :hide} :node/value)
  => :hide)

(fact "deprocess-assoc-data"
  (deprocess-assoc-data {} :node/value "hello" d1-env)
  => {}

  (deprocess-assoc-data {} :node/value "hello"
                        (assoc d1-env :view #{:node/value}))
  => {:node/value "hello"}
  (deprocess-assoc-data {} :node/value "hello"
                        (assoc-in d1-env [:deprocess :data-default] :show))
  => {:node/value "hello"})

(fact "deprocess-ref"
  (deprocess-ref :node/parent {:node/value "hello"}
                 (-> d1-fgeni :node/parent first)
                 (assoc d1-env :view #{:node/value})
                 #{})
  => nil

  (deprocess-ref :node/parent {:node/value "hello"}
                 (-> d1-fgeni :node/parent first)
                 (assoc d1-env :view #{:node/parent})
                 #{})
  => {}

  (deprocess-ref :node/parent {:node/value "hello"}
                 (-> d1-fgeni :node/parent first)
                 (assoc d1-env :view #{:node/parent :node/value})
                 #{})
  => {:+ {:node/value "hello"}})

(fact "deprocess-assoc"
  (deprocess-assoc {} :node/value "hello"
                   (-> d1-fgeni :node/value first)
                   d1-env
                   #{})
  => {}

  (deprocess-assoc {} :node/value "hello"
                   (-> d1-fgeni :node/value first)
                   (assoc d1-env :view #{:node/value})
                   #{})
  => {:node/value "hello"}

  (deprocess-assoc {} :node/value "hello"
                   (-> d1-fgeni :node/value first)
                   (assoc-in d1-env [:deprocess :data-default] :show)
                    #{})
  => {:node/value "hello"})

#_(fact
  (deprocess {:node/value "hello"} d1-env)
  => {:node {:value "hello"}}

  (deprocess {:node/parent {:node/value "hello"}}
             (assoc d1-env :deprocess {:data-default :show
                                       :refs-default :ids}
                    :view #{:node/parent}))
  => {:node {:parent {:value "hello"}}}


  (deprocess {:node/_parent #{{:node/value "hello"}}}
             (assoc d1-env :deprocess {:data-default :show
                                       :refs-default :ids}
                    :view #{:node/children}))
  => {:node {:children #{{:value "hello"}}}})
















#_(fact
  (deprocess {:node/parent
              {:db/id 1
               :node/value "hello"}}
             {:data {:node/value :show}
              :refs {:node/parent :show}
              :revs {}}
             s6-enva)
  => {:node {:parent {:+ {:db {:id 1}}
                      :value "hello"}}}

  (deprocess {:node/parent
              {:db/id 1
               :node/value "hello"}}
             {:data {:node/value :hide}
              :refs {:node/parent :show}
              :revs {}}
             s6-enva)
  => {:node {:parent {:+ {:db {:id 1}}}}}

  (deprocess-view-refs {:node/parent {:node/value "hello"}}
                       {:node/parent :show}
                       {:data {:node/value :show}
                        :refs {:node/parent :show}
                        :revs {}}
                       s6-enva
                       #{}))

(comment


  (def s8-env
    (process-init-env {:node {:value  [{:default "undefined"}]
                              :parent [{:type :ref
                                        :ref  {:ns :node
                                               :rval :children}}]}
                       :leaf {:value [{:default "leafy"}]
                              :node  [{:type :ref
                                       :ref {:ns :node
                                             :rval :leaves}}]}}))

  (def s8-fgeni (-> s8-env :schema :fgeni))

  (fact "view"
    (view s8-fgeni :leaf {:refs :show})
    => {:data {:leaf/value :show}
        :refs {:leaf/node :show}
        :revs {}}

    (view s8-fgeni {:node/children :show :leaf {}})
    => {:data {:node/value :show :leaf/value :show}
        :refs {:node/parent :ids :leaf/node :ids}
        :revs {:node/leaves :hide :node/children :show}}

    (view s8-fgeni)
    => {:data {:node/value :show :leaf/value :show}
        :refs {:node/parent :ids :leaf/node :ids}
        :revs {:node/leaves :hide :node/children :hide}}

    (keys s8-fgeni)
    => (just [:node/value   :node/parent
              :node/leaves  :node/children
              :leaf/value   :leaf/node]
             :in-any-order)

    (list-keyword-ns s8-fgeni)
    => #{:leaf :node}

    (view-keyword s8-fgeni :leaf {})
    => {:data {:leaf/value :show}
        :refs {:leaf/node :ids}
        :revs {}}

    (view-keyword s8-fgeni :node {})
    => {:data {:node/value :show}
        :refs {:node/parent :ids}
        :revs {:node/children :hide
               :node/leaves :hide}}

    (view-hashmap s8-fgeni {:node/children :show})
    => {:revs {:node/children :show}}

    (view-hashmap s8-fgeni {:node/children :show
                            :leaf/value :hide})
    => {:data {:leaf/value :hide}
        :revs {:node/children :show}})

  (fact "view"
    (view (-> s6-env :schema :fgeni) :node)
    => {:data {:node/value :show}
        :refs {:node/parent :ids}
        :revs {:node/children :hide}}

    (view (-> s6-env :schema :fgeni) :node {:data :hide :ref :show})
    => {:data {:node/value :hide}
        :refs {:node/parent :ids}
        :revs {:node/children :hide}}

    (view (-> s6-env :schema :fgeni) {:node {:children :show}})
    => {:data {:node/value :show}
        :refs {:node/parent :ids}
        :revs {:node/children :show}}


    (view (-> s6-env :schema :fgeni) :node {:revs :show})
    => {:data {:node/value :show}
        :refs {:node/parent :ids}
        :revs {:node/children :show}}


    (view (-> s5-env :schema :fgeni))
    => {:data {:nsA/sub2/val :show
               :nsB/sub1/val :show
               :nsB/sub2/val :show
               :nsA/sub1/val :show
               :nsB/val1 :show
               :nsB/val2 :show
               :nsA/val1 :show
               :nsA/val2 :show}, :refs {}, :revs {}}

    (view (-> s5-env :schema :fgeni) #{:nsA :nsB})
    => {:data {:nsB/val1 :show
               :nsB/val2 :show
               :nsA/val1 :show
               :nsA/val2 :show}
        :refs {}
        :revs {}}


    (view (-> s5-env :schema :fgeni) {:nsA {}})
    => {:data {:nsA/val1 :show
               :nsA/val2 :show}
        :refs {}
        :revs {}}


    (view (-> s5-env :schema :fgeni) {:nsA {:val1 :hide
                                            :sub1 {:val :hide}}})
    => {:data {:nsA/sub1/val :hide
               :nsA/val1 :hide
               :nsA/val2 :show}
        :refs {}
        :revs {}})

  (fact "links"
    (links {:data {:node/value :show :leaf/value :show}
            :refs {:node/parent :ids :leaf/node :ids}
            :revs {:node/leaves :hide :node/children :show}})
    => {:node {:node/children :show, :node/parent :ids}
        :leaf {:leaf/node :ids}})
)



#_(fact "deprocess"
  (deprocess-assoc :nsA/val1 "A1"
                   (flatten-keys-in (emit-view (-> s5-env :schema :geni) :nsA))
                   s5-env
                   #{}
                   {})
  => {:nsA {:val1 "A1"}}

  (deprocess {:db/id 1} {})
  => {:db {:id 1}}

  (deprocess {:db/id 1 :nsA/val1 "A1"} s5-env)
  => {:db {:id 1}
      :nsA {:val1 "A1"}}

  (deprocess {:db/id 0 :node/parent {:db/id 1 :node/value "root"}} s6-env)
  => {:node {:parent {:+ {:db {:id 1}}}} :db {:id 0} }

  (deprocess {:db/id 0 :node/parent {:db/id 1 :node/value "root"}}
             {:data {:node/value :show}
              :refs {:node/parent :id}}
             s6-env)
  => {:node {:parent {:+ {:db {:id 1}}}}, :db {:id 0}}


  (deprocess {:db/id 0 :node/parent {:db/id 1 :node/value "root"}}
             {:data {:node/value :show}
              :refs {:node/parent :show}}
             s6-env)
  => {:node {:parent {:+ {:db {:id 1}}, :value "root"}}, :db {:id 0}}

  (deprocess {:node/value "l2"
              :node/parent {:node/value "l1"
                            :node/parent {:node/value "root"}}}
             {:data {:node/value :show}
              :refs {:node/parent :show}}
             s6-env)
  => {:node {:value "l2", :parent {:value "l1", :parent {:value "root"}}}}

  (deprocess {:node/value "root"
              :node/children #{{:node/value "l1"
                                :node/children #{{:node/value "l2"}}}}}
             {:data {:node/value :show}
              :refs {:node/children :show}}
              s6-env)
  => {:node {:value "root", :children #{{:value "l1", :children #{{:value "l2"}}}}}}

  (deprocess {:node/value "l2"
              :node/parent {:node/value "l1"}}
             {:refs {:node/parent :show}}
              s6-env)
  => {:node {:parent {}}})

#_(fact "reverse-lookup"
  (deprocess {:node/value "root"
              :node/_parent #{{:node/value "l1"
                               :node/_parent #{{:node/value "l2"}}}}}
             {:refs {:node/children :show}
              :data {:node/value :show}}
             s6-env)
  => {:node {:value "root", :children #{{:value "l1", :children #{{:value "l2"}}}}}}

  (deprocess {:node/value "root"
              :node/_parent #{{:node/value "l1"
                               :node/_parent #{{:node/value "l2"}}}}}
             {:refs {:node/children :show}}
             s6-env)
  => {:node {:children #{{:children #{{}}}}}})
