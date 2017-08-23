(ns spirit.rabbitmq.request
  (:require [hara.component :as component]
            [hara.string.case :as case]
            [hara.data.nested :as nested]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(def ^:dynamic *default-request-options*
  {:accept :json
   :content-type "application/json"
   :throw-exceptions false})

(defn create-url
  "creates the management url
 
   (create-url rabbitmq/*default-options* \"hello\")
   => \"http://localhost:15672/api/hello\""
  {:added "0.5"}
  [{:keys [protocol host management-port]} suburl]
  (str protocol "://" host ":" management-port "/api/" suburl))

(defn wrap-parse-json
  "returns the body as a clojure map
 
   ((wrap-parse-json atom)
    {:status 200
     :body (json/generate-string {:a 1 :b 2})})
   => {:a 1, :b 2}"
  {:added "0.5"}
  [f]
  (fn [request]
    (let [res @(f request)]
      (cond (= 200 (:status res))
            (json/parse-string (:body res) (comp case/spear-case keyword))
            
            (= 204 (:status res))
            true

            (<= 400 (:status res))
            (throw (ex-info "HTTP Error" res))

            :else res))))

(defn update-nested-keys
  "updates keys in the nesting
   
   (update-nested-keys {:a {:b {:c 1}}}
                       #(keyword (str (name %) \"-boo\")))
   => {:a-boo {:b-boo {:c-boo 1}}}"
  {:added "0.5"}
  [m func]
  (cond (map? m)
        (reduce-kv (fn [out k v]
                     (assoc out
                            (func k)
                            (if (coll? v)
                              (update-nested-keys v func)
                              v)))
                   {}
                   m)

        (coll? m)
        (into (empty m) (map #(update-nested-keys % func) m))))

(defn wrap-generate-json
  "returns the body as a json string
   
   ((wrap-generate-json identity)
    {:status 200
     :body {:a 1 :b 2}})
   => {:status 200, :body \"{\\\"a\\\":1,\\\"b\\\":2}\"}"
  {:added "0.5"}
  [f]
  (fn [request]
    (let [body (:body request)
          body (if body
                 (-> body
                     (update-nested-keys (comp case/snake-case name))
                     (json/encode)))
          request (if body
                    (assoc request :body body)
                    request)]
      (f request))))

(defn request
  "creates request for the rabbitmq management api
 
   (request rabbitmq/*default-options* \"cluster-name\")
   => (contains {:name string?})  "
  {:added "0.5"}
  ([rabbit suburl]
   (request rabbit suburl :get))
  ([rabbit suburl method]
   (request rabbit suburl method {}))
  ([{:keys [username password] :as rabbit} suburl method opts]
   (let [req (nested/merge-nested {:headers {"Content-Type" "application/json"}
                                   :url (create-url rabbit suburl)
                                   :method method
                                   :basic-auth [username password]}
                                  *default-request-options*
                                  opts)]
     ((-> http/request
          wrap-generate-json
          wrap-parse-json)
      req))))
