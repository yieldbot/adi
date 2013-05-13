(ns adi.emit.test-characterise
  (:use midje.sweet
        adi.emit.characterise
        adi.utils
        [hara.common :exclude [hash-map?]]
        [adi.emit.process :only [process]]
        adi.data.samples))

(fact "characterise-nout"
  (characterise-nout :db {:id 0} s7-env {})
  => {:db {:id 0}}

  (characterise-nout :account/name "chris" s7-env {})
  => {:data-one {:account/name "chris"}}

  (characterise-nout :account/tags #{"t1"} s7-env {})
  {:data-many {:account/tags #{"t1"}}}

  (characterise-nout :node/parent {:node/value "parent1"} s6-env {})
  => {:refs-one {:node/parent {:data-one {:node/value "parent1"}}}}

  (characterise-nout :node/children #{{:node/value "child1"}} s6-env {})
  => {:refs-many {:node/_parent #{{:data-one {:node/value "child1"}}}}})

(fact "characterise"
  (characterise {:account/name "chris"} s7-env)
  => {:data-one {:account/name "chris"}}

  (characterise {:node/value "undefined"
                 :node/children #{}}
                s6-env)
  => {:refs-many {:node/_parent #{}}
      :data-one {:node/value "undefined"}}

  (characterise {:node/value "undefined"
                 :node/children #{{:node/value "child1"}}
                 :node/parent {:node/value "parent1"}}
                s6-env)

  => {:data-one {:node/value "undefined"}
      :refs-many {:node/_parent #{{:data-one {:node/value "child1"}}}}
      :refs-one {:node/parent {:data-one {:node/value "parent1"}}}}

  (characterise {:account/name #{"chris"}} s6-env)
  => (throws Exception)
)

(fact "characterise-gen-id"
  (characterise-gen-id {} {})
  => {}

  (characterise-gen-id {} {:generate {:ids {:function (constantly 1)}}})
  => {:db {:id 1}}

  (characterise-gen-id {:db {:id 3}} {:generate {:ids {:function (constantly 1)}}})
  => {:db {:id 3}})

(fact "characterise-gen-sym"
  (characterise-gen-sym {} {})
  => {}

  (characterise-gen-sym {} {:generate {:syms {:function (constantly '?e)}}})
  => {:# {:sym '?e}}

  (characterise-gen-sym {:# {:sym '?x}} {:generate {:syms {:function (constantly '?e)}}})
  => {:# {:sym '?x}})

(fact "characterise refs"
  (characterise
   (process {:ns1/prev {:value "hello"}} s2-env)
   (merge-nested s2-env {:generate {:ids {:function (incremental-id-gen)}}}))
  =>  {:# {:nss #{:ns1}}, :db {:id 1}
       :refs-many
       {:ns2/_next
        #{{:# {:nss #{:ns2}}, :db {:id 2}
           :data-one {:ns2/value "hello"}}}}}

  (characterise
   (process {:ns1/next {:next {:value "hello"}}} s2-env)
   (merge-nested s2-env {:generate {:ids {:function (incremental-id-gen)}}}))
  {, :# {:nss #{:ns1}}, :db {:id 1}
   :refs-one
   {:ns1/next
    {:refs-one {:ns2/next {:data-one {:ns1/value "hello"}, :# {:nss #{:ns1}}, :db {:id 3}}}, :# {:nss #{:ns2}}, :db {:id 2}}}}

  (characterise
   (process {:ns1/next {:next {:value "hello"}}} s2-env)
   (merge-nested s2-env {:generate {:syms {:function (incremental-sym-gen 'n)}}}))
  => '{:# {:nss #{:ns1}, :sym ?n1}
       :refs-one
       {:ns1/next {:# {:nss #{:ns2}, :sym ?n2}
                   :refs-one
                   {:ns2/next {:# {:nss #{:ns1}, :sym ?n3}
                               :data-one {:ns1/value "hello"}}}}}})
