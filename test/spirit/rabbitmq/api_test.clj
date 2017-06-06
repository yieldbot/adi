(ns spirit.rabbitmq.api-test
  (:use hara.test)
  (:require [spirit.rabbitmq.api :refer :all]))

^{:refer spirit.rabbitmq.http.api/classify-args :added "0.5"}
(fact "classify-args for generating api string"

  (classify-args "hello")
  => [:string "hello"]

  (classify-args "{hello}")
  => [:keyword '(:hello rabbitmq)]

  (classify-args "{%1:hello}")
  => [:entry [1 "hello"]])

^{:refer spirit.rabbitmq.http.api/build-args :added "0.5"}
(fact "build args for function array"

  (build-args [[:string "hello"]
               [:entry [1 "world"]]
               [:keyword '(:hello rabbitmq)]])
  => ["hello" 'world '(:hello rabbitmq)])

^{:refer spirit.rabbitmq.http.api/links-args :added "0.5"}
(fact "build args for link"

  (link-args "hello/{world}/{%1:foo}/{%2:bar}")
  => {:inputs '["hello" (:world rabbitmq) foo bar]
      :vargs  '[foo bar]})

^{:refer spirit.rabbitmq.http.api/create-link-form :added "0.5"}
(fact "creates link form from vargs"

  (create-link-form '{:inputs ["hello" (:world rabbitmq) foo bar]
                      :vargs  [foo bar]}
                    :delete)
  => '([rabbitmq foo bar]
       (spirit.rabbitmq.http.request/request
        rabbitmq
        (clojure.string/join "/"
                             ["hello"
                              (:world rabbitmq)
                              foo bar])
        :delete)))

^{:refer spirit.rabbitmq.http.api/create-body-form :added "0.5"}
(fact "creates body form from vargs"

  (create-body-form '{:inputs ["hello" (:world rabbitmq) foo bar]
                      :vargs  [foo bar]}
                    :post)
  
  => '([rabbitmq foo bar body]
       (spirit.rabbitmq.http.request/request
        rabbitmq
        (clojure.string/join "/"
                             ["hello"
                              (:world rabbitmq)
                              foo bar])
        :post
        {:body body})))

^{:refer spirit.rabbitmq.http.api/create-accessor-form :added "0.5"}
(fact "creates the accessor form"

  (create-accessor-form :cluster-name
                        {:link "cluster-name"
                         :methods {:setter :put}})
  => '(clojure.core/defn cluster-name
        ([rabbitmq]
         (spirit.rabbitmq.http.request/request
          rabbitmq (clojure.string/join "/" ["cluster-name"])
          :get))
        ([rabbitmq body]
         (spirit.rabbitmq.http.request/request
          rabbitmq (clojure.string/join "/" ["cluster-name"])
          :put {:body body}))))

^{:refer spirit.rabbitmq.http.api/create-function-form :added "0.5"}
(fact "creates all forms for functions"

  (->> (create-function-forms :vhost
                              {:type :form
                               :link "vhosts/{%1:vhost}"
                               :methods #{:get :put :delete}})
       (map second))
  => '(get-vhost delete-vhost add-vhost))

^{:refer spirit.rabbitmq.http.api/create-api-functions :added "0.5"}
(comment "creates all form and accessor functions for rabbitmq api"

  (create-api-functions methods))
