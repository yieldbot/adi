(ns spirit.rabbitmq.http.api
  (:require [spirit.rabbitmq.http.request :as request]
            [clojure.string :as string])
  (:refer-clojure :exclude [methods]))

(def ^:dynamic *default-methods* {:getter :get})

(def spec
  {"overview"                     {:methods #{:get}}
   "cluster-name"                 {:methods {:get {:action "get"}
                                             :put {:action "set"
                                                   :spec {:name :<cluster-name>}}}}
   
   "nodes"                        {:methods {:get {:action :list
                                                   :fn #(map :name %)}}}
   "nodes/{%1:name}"              {:methods #{:get}}
   
   "extensions"                   {:methods #{:get}}
   "definitions"                  {:methods #{:get :post}}

   "connections"                       {:methods #{:get}}
   "connections/{%1:name}"             {:methods #{:get :delete}}
   "connections/{%1:name}/channels"    {:name "channels-from"
                                        :methods #{:get}}

   "channels"                     {:methods #{:get}}
   "channels/{%1:name}"           {:methods #{:get}}

   "consumers"                    {:methods #{:get}}
   "consumers/{vhost}"            {:methods #{:get}}

   "exchanges"                    {:methods #{:get}}
   "exchanges/{vhost}"            {:methods #{:get}}
   "exchanges/{vhost}/{%1:name}"       {:methods #{:get :post :delete}}
   "exchanges/{vhost}/{%1:name}/bindings/source"        {:methods #{:get}}
   "exchanges/{vhost}/{%1:name}/bindings/destination"   {:methods #{:get}}
   "exchanges/{vhost}/{%1:name}/publish"                {:methods #{:post}}
   
   "queues"                       {:methods #{:get}}
   "queues/{vhost}"               {:methods #{:get}}

   ;; :put {"auto_delete":false,"durable":true,"arguments":{},"node":"rabbit@smacmullen"}
   ;; :delete :query-params if-empty=true/if-unused=true
   "queues/{vhost}/{%1:name}"          {:methods #{:get :put :delete}} 

   "queues/{vhost}/{%1:name}/bindings"    {:methods #{:get}}
   "queues/{vhost}/{%1:name}/contents"    {:methods #{:delete}}
   "queues/{vhost}/{%1:name}/actions"     {:methods #{:post}}  ;; {"action":"sync"}
   "queues/{vhost}/{%1:name}/get"         {:methods #{:post}}  ;; {"count":5,"requeue":true,"encoding":"auto","truncate":50000}

   "bindings"                               {:methods #{:get}}
   "bindings/{vhost}"                       {:methods #{:get}}
   "bindings/{vhost}/e/{%1:source}/q/{%2:dest}"         {:methods #{:get :post}}
   "bindings/{vhost}/e/{%1:source}/q/{%2:dest}/props"   {:methods #{:get :delete}}
   
   "bindings/{vhost}/e/{%1:source}/e/{%2:dest}"         {:methods #{:get :post}}
   "bindings/{vhost}/e/{%1:source}/e/{%2:dest}/props"   {:methods #{:get :delete}}
   
   "vhosts"                                  {:methods #{:get}}
   "vhosts/{%1:vhost}"                       {:methods #{:get :put :delete}}
   "vhosts/{%1:vhost}/permissions"           {:methods #{:get}}

   "users"                                   {:methods #{:get}}
   "users/{%1:name}"                         {:methods #{:get :put :delete}}  ;; :put {"password":"secret","tags":"administrator"}
   "users/{%1:name}/permissions"             {:methods #{:get}}
   "whoami"                                  {:methods #{:get}}

   "permissions"                             {:methods #{:get}}
   "permissions/{vhost}/{%1:user}"           {:methods #{:get :put :delete}}

   "parameters"                              {:methods #{:get}}
   "parameters/{%1:param}"                   {:methods #{:get}}
   "parameters/{%1:param}/{vhost}"           {:methods #{:get}}
   "parameters/{%1:param}/{vhost}/{%2:name}" {:methods #{:get :put :delete}}

   "policies"                                {:methods #{:get}}
   "policies/{vhost}"                     {:methods #{:get}}
   "policies/{vhost}/{%1:name}"           {:methods #{:get :put :delete}}

   "aliveness-test/{vhost}"               {:methods #{:get}}})

(def methods
  {:overview        {:link  "overview"}
   :cluster-name    {:link "cluster-name"
                     :methods {:setter :put}}
   :extensions      {:link "extensions"}
   :definitions     {:link "definitions"
                     :methods {:setter :post}}
   
   :get-node        {:link "nodes/{%1:name}"}
   :list-nodes      {:link "nodes"}

   :vhost           {:type :form
                     :link "vhosts/{%1:vhost}"
                     :methods #{:get :put :delete}}
   :list-vhosts     {:link "vhosts"}
   
   :queue           {:type :form
                     :link "queues/{vhost}/{%1:name}"
                     :methods #{:get :put :delete}}
   :list-queues     {:link "queues/{vhost}"}
   ;;:all-queues      {:link "queues"}
   
   :exchange        {:type :form
                     :link "exchanges/{vhost}/{%1:name}"
                     :methods #{:get :put :delete}}
   :list-exchanges  {:link "exchanges/{vhost}"}
   ;;:all-exchanges   {:link "exchanges"}
   
   :permissions     {:type :form
                     :link "permissions/{vhost}/{%1:user}"
                     :methods #{:get :put :delete}}
   :list-permissions {:link "permissions"}

   :bind-exchange   {:link "bindings/{vhost}/e/{%1:source}/e/{%2:dest}"
                     :methods {:setter :post}}
   :bind-queue      {:link "bindings/{vhost}/e/{%1:source}/q/{%2:dest}"
                     :methods {:setter :post}}
   :list-bindings   {:link "bindings/{vhost}"}
   :all-bindings    {:link "bindings"}

   :list-connections {:link "connection"}
   :connection      {:link "connection/{%1:name}"
                     :methods #{:get :delete}}

   :channels-in     {:link "connections/{%1:connection}/channels"}

   :list-channels    {:link "channels"}
   :get-channel     {:link "channels/{%1:name}"}

   ;;:list-consumers   {:link "consumers"}
   :list-consumers   {:link "consumers/{vhost}"}
   
   :user            {:type :form
                     :link "users/{%1:name}"
                     :methods #{:get :put :delete}}  ;; {"password":"secret","tags":"administrator"}
   :list-users      {:link "users"}
   :healthcheck     {:link "aliveness-test/{vhost}"}})

(defn classify-args [s]
    (cond (and (.startsWith s "{")
               (.endsWith s "}"))
          (let [s (subs s 1 (dec (count s)))]
            (cond (.startsWith s "%")
                  (let [s (subs s 1)
                        [num name] (string/split s #":")]
                    [:entry [(Integer/parseInt num) name]])
                  
                  :else
                  [:keyword (list (keyword s) 'rabbitmq)]))
          
          :else
          [:string s]))

(defn build-args [args]
  (mapv (fn [[t data]]
          (cond (= t :entry)
                (symbol (second data))

                :else
                data))
        args))

(defn link-args [uri]
  (let [args (->> (string/split uri #"/")
                  (map classify-args))
        entries (->> args
                     (filter #(-> % first (= :entry)))
                     (sort-by #(-> % second first)))]
    {:inputs (build-args args)
     :vargs  (mapv (comp symbol second second) entries)}))

(defn create-link-form [{:keys [inputs vargs]} key]
  (list (vec (cons 'rabbitmq vargs))
        (list `request/request 'rabbitmq (list `string/join "/" inputs) key)))

(defn create-body-form [{:keys [inputs vargs]} key]
  (list (conj (vec (cons 'rabbitmq vargs)) 'body)
        (list `request/request 'rabbitmq (list `string/join "/" inputs) key {:body 'body})))

(defn create-accessor-form
  [fname {:keys [link methods spec]}]
  (let [args        (link-args link)
        getter-key  (or (:getter methods) :get)
        getter-form (create-link-form args getter-key)
        setter-form (if-let [setter-key (:setter methods)] 
                      (create-body-form args setter-key))]
    `(defn ~(symbol (if (keyword? fname)
                      (name fname)
                      fname))
       ~@(filter identity [getter-form setter-form]))))

(defn create-function-forms
  [fname {:keys [link methods]}]
  (let [args  (link-args link)
        fname (if (keyword? fname)
                (name fname)
                fname)
        forms {:get    (create-link-form args :get)
               :delete (create-link-form args :delete)
               :put    (create-body-form args :put)
               :post   (create-body-form args :post)}
        prefix {:get :get :delete :delete :put :add :post :add}]
    (reduce (fn [arr k]
              (conj arr `(defn ~(symbol (str (name (get prefix k)) "-" fname))
                           ~(get forms k))))
            []
            methods)))

(defn create-api-functions [methods]
  (mapv (fn [[name opts]]
          (eval (cond (= :form (:type opts))
                      (create-function-forms name opts)

                      :else
                      (create-accessor-form name opts))))
        (seq methods)))

(create-api-functions methods)
