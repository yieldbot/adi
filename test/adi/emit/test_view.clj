(ns adi.emit.test-view
  (:use midje.sweet
        adi.schema
        adi.data.samples
        adi.emit.view))

(fact "view-nval"
  (view-nval d1-fgeni :node/value {})
  => :show

  (view-nval d1-fgeni :node/value {:data :hide})
  => :hide

  (view-nval d1-fgeni :node/parent {})
  => :ids

  (view-nval d1-fgeni :node/parent {:refs :show})
  => :show

  (view-nval d1-fgeni :node/children {})
  => :hide

  (view-nval d1-fgeni :node/children {:revs :ids})
  => :ids)

(fact "view-loop"
  (view-loop d1-fgeni #{:node/value :node/parent} {})
  => {:node/value :show
      :node/parent :ids}

  (view-loop d1-fgeni #{:node/value :node/parent :node/children}
             {:data :hide :refs :show :revs :ids})
  => {:node/value :hide
      :node/children :ids
      :node/parent :show})

(fact "view-keyword"
  (view-keyword d1-fgeni :node {})
  => {:node/parent :ids
      :node/leaves :hide
      :node/children :hide
      :node/value :show}

  (view-keyword d1-fgeni :node {:data :hide :refs :show :revs :ids})
  => {:node/parent :show
      :node/leaves :ids
      :node/children :ids
      :node/value :hide})

(fact "view-hashset"
  (view-hashset d1-fgeni #{:leaf} {})
  => {:leaf/node :ids, :leaf/value :show}

  (view-hashset d1-fgeni #{:leaf :node}
                {:data :hide :refs :show :revs :ids})
  => {:node/value :hide
      :node/children :ids
      :node/leaves :ids
      :node/parent :show
      :leaf/node :show
      :leaf/value :hide})

(fact "view-hashmap"
  (view-hashmap d1-fgeni {:leaf {:node :show}} {})
  => {:leaf/node :show, :leaf/value :show}

  (view-hashmap d1-fgeni {:leaf/node :show} {})
  => {:leaf/node :show, :leaf/value :show}

  (view-hashmap d1-fgeni {:leaf {}} {})
  => {:leaf/node :ids, :leaf/value :show}

  (view-hashmap d1-fgeni {:leaf {:node :show}
                          :node/value :hide} {})
  => {:node/value :hide
      :node/children :hide
      :node/leaves :hide
      :node/parent :ids
      :leaf/node :show
      :leaf/value :show})

(fact "view"
  (view d1-fgeni)
  => {:node/value :show
      :node/children :hide
      :node/leaves :hide
      :node/parent :ids
      :leaf/node :ids
      :leaf/value :show}

  (view d1-fgeni {:leaf/node :show})
  => {:leaf/node :show
      :leaf/value :show}

  (view d1-fgeni {:leaf/l :show})
  => {:leaf/node :ids, :leaf/value :show}

  (view d1-fgeni {:leaf {}} {:refs :show})
  => {:leaf/node :show, :leaf/value :show}
  )

(fact "view-make-set"
  (view-make-set (view d1-fgeni {:leaf/node :show :node/parent :show}))
  => #{:node/parent :leaf/node :leaf/value :node/value}

  (view-make-set (view d1-fgeni {:leaf/l :show}))
  => #{:leaf/value})
