(ns spirit.network.base.hub-test
  (:use hara.test)
  (:require [spirit.network.base.hub :refer :all]
            [spirit.network.common :as common]))

^{:refer spirit.network.base.hub/hub :added "0.5"}
(fact "creates a hub in order to connect multiple endpoints to"

  (def sys (hub {:format :edn
                 :options {:time  true
                           :track true}
                 :default {:params {:full true
                                    :metrics true}}
                 :return  {:type    :channel
                           :timeout 1000}
                 :handlers  {:on/id (fn [req] :a)}})))

^{:refer spirit.network.base.hub/connect :added "0.5"}
(fact "returns an endpoint for connection"

  (def ca (connect sys))

  (Thread/sleep 10)
  
  (common/request ca :on/id nil {:params {}})
  => :a)

^{:refer spirit.network.base.hub/list-connections :added "0.5"}
(comment "returns all active connections"

  (list-connections sys)
  ;;=> ("9879eab5-b78f-4bfc-b341-08c9a80b6ce5"
  ;;    "fa07e87f-0c34-4618-9a96-1d43b930f7c8")
  )

^{:refer spirit.network.base.hub/close-connection :added "0.5"}
(comment "returns all active connections"

  (close-connection sys "9879eab5-b78f-4bfc-b341-08c9a80b6ce5"))
  
