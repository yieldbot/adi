(ns adi.emit.test-reap
  (:use midje.sweet
        adi.schema
        adi.data.samples
        adi.emit.reap))

(fact "initial lookup"
  (-> d1-env :schema :lu)
  => {:node/parent :node/parent
      :node/value :node/value
      :leaf/node :leaf/node
      :leaf/value :leaf/value
      :node/children :node/children
      :node/_parent :node/children
      :node/leaves :node/leaves
      :leaf/_node :node/leaves})

(fact "reap-init-env"
  (reap-init-env {})
  => {}

  (reap-init-env {:view :node})

  (select-keys (reap-init-env (assoc d1-env :view [:node/value]))
               [:view])
  {:view {:node/value :show}}

  (reap-init-env {:view {:node/value :show}})
  => {:view {:node/value :show}})


(fact "reap-init-create-ns-view"
  (reap-init-create-view :node d1-env)
  => {:node/parent   :show
      :node/value    :show}

  (reap-init-create-view :node d1-env)
  => {:node/value :show, :node/parent :show}

  (reap-init-create-view [:node] d1-env)
  => {:node/value :show, :node/parent :show}

  (reap-init-create-view #{:node/value :node/parent} d1-env)
  => {:node/parent :follow, :node/value :show}

  (reap-init-create-field-view #{:node/value [:node/parent :follow]} d1-env)
  => {:node/parent :follow, :node/value :show})


(fact "reap-assoc-data"
  (reap-assoc-data {} :node/value "hello" d1-env)
  => {}

  (reap-assoc-data {} :node/value "hello"
                   (assoc d1-env :view {:node/value :show}))
  => {:node/value "hello"}

  (reap-assoc-data {} :node/value "hello"
                   (assoc d1-env :reap {:data :show}))
  => {:node/value "hello"}

  (reap-assoc-data {} :node/value "hello"
                   (assoc d1-env
                     :view {:node/value :hide}
                     :reap {:data :show}))
  => {})

(fact "reap-ref without id"
  (reap-ref :node/parent {:node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/value :show})
            #{})
  => nil

  (reap-ref :node/parent {:node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :show})
            #{})
  => {}

  (reap-ref :node/parent {:node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :follow})
            #{})
  => {}

  (reap-ref :node/parent {:node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :follow
                                 :node/value  :show})
            #{})
  => {:value "hello"})


(fact "reap-ref with id"
  (reap-ref :node/parent {:db/id 1}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :show})
            #{})
  => {:+ {:db {:id 1}}}

  (reap-ref :node/parent {:db/id 1 :node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :show})
            #{})
  => {:+ {:db {:id 1}}}

  (reap-ref :node/parent {:db/id 1 :node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :follow})
            #{})
  => {:+ {:db {:id 1}}}

  (reap-ref :node/parent {:db/id 1 :node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :follow
                                 :node/value  :show})
            #{})
  => {:+ {:db {:id 1}} :value "hello"}

  (reap-ref :node/parent {:db/id 1 :node/value "hello"}
            (-> d1-fgeni :node/parent first)
            (assoc d1-env :view {:node/parent :follow}
                   :reap {:data :show})
            #{})
  => {:+ {:db {:id 1}} :value "hello"})

(fact "reap-assoc-ref"
  (reap-assoc-ref {} :node/parent {:node/value "hello"}
                  (-> d1-fgeni :node/parent first)
                  (assoc d1-env :view {:node/parent :follow
                                       :node/value :show})
                  #{})
  => {:node/parent {:value "hello"}}

  (reap-assoc-ref {} :node/children #{{:node/value "hello"}}
                  (-> d1-fgeni :node/children first)
                  (assoc d1-env :view {:node/children :follow})
                  #{})
  => {:node/children #{{}}}

  (reap-assoc-ref {} :node/children #{{:node/value "hello"}}
                  (-> d1-fgeni :node/children first)
                  (assoc d1-env :view {:node/children :follow
                                       :node/value :show})
                  #{})
  => {:node/children #{{:value "hello"}}})


(fact "reap-entity"
  (reap-entity {:db/id 1} d1-env #{})
  => {}

  (reap-entity {} {:db/id 1} d1-env #{})
  => {}

  (reap-entity {:node/value "hello"}
                d1-env #{})
  => {}

  (reap-entity {:node/value "hello"}
                (assoc d1-env :view {:node/value :show}) #{})
  => {:node/value "hello"}

  (reap-entity {:db/id 0 :node/parent {:db/id 1 :node/value "hello"}}
                (assoc d1-env :view {:node/value :show
                                     :node/parent :follow}) #{})
  => {:node/parent {:+ {:db {:id 1}}, :value "hello"}}

  (reap-entity {:db/id 0 :node/parent {:db/id 1 :node/value "hello"}}
                (assoc d1-env :view {:node/value :show
                                     :node/parent :follow}) #{1})
  => {:node/parent {:+ {:db {:id 1}}}}

  (reap-entity {:db/id 0 :node/parent {:db/id 1 :node/value "hello"}}
                (assoc d1-env :view {:node/value  :show
                                     :node/parent :follow}) #{0 1})
  => {:node/parent {:+ {:db {:id 1}}}})


(fact "reap"
  (reap {:db/id 0 :node/_parent {:db/id 1 :node/value "hello"}}
        (assoc d1-env :view {:node/value  :show
                             :node/children :follow}))
  => {:db {:id 0}, :node {:children {:+ {:db {:id 1}}, :value "hello"}}}

  (reap {:db/id 0 :node/_parent {:db/id 1 :node/value "hello"}}
        (assoc d1-env :view {:node/value  :show
                             :node/children :follow}
               :reap {:ids :hide}))
  => {:node {:children {:value "hello"}}})
