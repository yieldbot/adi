(defproject im.chit/spirit "0.7.1"
  :description "data. simplified"
  :url "https://www.github.com/zcaudate/spirit"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :aliases {"test" ["run" "-m" "hara.test" "exit"]}
  :dependencies [[org.clojure/clojure    "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [im.chit/hara.common    "2.5.10"]
                 [im.chit/hara.component "2.5.10"]
                 [im.chit/hara.data      "2.5.10"]
                 [im.chit/hara.event     "2.5.10"]
                 [im.chit/hara.function  "2.5.10"]
                 [im.chit/hara.string    "2.5.10"]
                 [com.datomic/datomic-free "0.9.5561.50"]
                 
                 [inflections "0.13.0"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.0"]
                 [cheshire "5.7.1"]
                 
                 [ring/ring "1.6.1"]
                 [ring/ring-defaults "0.3.0"]
                 [org.eclipse.jetty.websocket/websocket-client "9.4.6.v20170531"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.136"]
                 [com.draines/postal     "2.0.2"]
                 [com.rabbitmq/amqp-client "4.1.1"]]
                
 :injections  [(require  '[lucid.core.inject :as inject]
                         'hara.test
                         'lucid.unit
                         'lucid.publish)
               (inject/in [lucid.core.inject :refer [inject [in inject-in]]]
                          [hara.io.project project]
                           
                          clojure.core
                          [lucid.mind .& .> .? .* .% .%> .>var .>ns])]
  :java-source-paths ["java"]
  :publish {:theme  "stark"
            
            :template {:site   "spirit"
                       :author "Chris Zheng"
                       :email  "z@caudate.me"
                       :icon   "favicon"
                       :tracking-enabled "true"
                       :tracking "UA-31320512-2"}

            :files {"index"
                    {:input "test/documentation/home_spirit.clj"
                     :title "home"
                     :subtitle "data. simplified."}
                    "spirit-datomic"
                    {:input "test/documentation/spirit_datomic.clj"
                     :title "datomic"
                     :subtitle "datomic API Reference"}
                    "spirit-httpkit"
                    {:input "test/documentation/spirit_httpkit.clj"
                     :title "httpkit"
                     :subtitle "httpkit API Reference"}
                    "spirit-rabbitmq"
                    {:input "test/documentation/spirit_rabbitmq.clj"
                     :title "rabbitmq"
                     :subtitle "rabbitmq API Reference"}
                    "spirit-data-atom"
                    {:input "test/documentation/spirit_data_atom.clj"
                     :title "data.atom"
                     :subtitle "data.atom API Reference"}
                    "spirit-data-cache"
                    {:input "test/documentation/spirit_data_cache.clj"
                     :title "data.cache"
                     :subtitle "data.cache API Reference"}
                    "spirit-data-exchange"
                    {:input "test/documentation/spirit_data_exchange.clj"
                     :title "data.exchange"
                     :subtitle "data.exchange API Reference"}
                    "spirit-data-graph"
                    {:input "test/documentation/spirit_data_graph.clj"
                     :title "data.graph"
                     :subtitle "data.graph API Reference"}
                    "spirit-data-keystore"
                    {:input "test/documentation/spirit_data_keystore.clj"
                     :title "data.keystore"
                     :subtitle "data.keystore API Reference"}
                    "spirit-data-pipeline"
                    {:input "test/documentation/spirit_data_pipeline.clj"
                     :title "data.pipeline"
                     :subtitle "data.pipeline API Reference"}
                    "spirit-data-schema"
                    {:input "test/documentation/spirit_data_schema.clj"
                     :title "data.schema"
                     :subtitle "data.schema API Reference"}
                    "spirit-data-table"
                    {:input "test/documentation/spirit_data_table.clj"
                     :title "data.table"
                     :subtitle "data.table API Reference"}
                    "spirit-network"
                    {:input "test/documentation/spirit_network.clj"
                     :title "network"
                     :subtitle "network API Reference"}}}
  
  :distribute {:jars  :dependencies
               :files [{:type :clojure
                        :levels 2
                        :path "src"
                        :standalone #{"common" "datomic" "httpkit" "network" "rabbitmq"}}]}
  
  :profiles {:dev {:dependencies [[im.chit/lucid.distribute "1.3.13"]
                                  [im.chit/lucid.mind       "1.3.13"]
                                  [im.chit/lucid.publish    "1.3.13"]
                                  [im.chit/lucid.unit       "1.3.13"]
                                  [im.chit/hara.test  "2.5.10"]
                                  [clj-time "0.13.0"]]
                   :plugins [[lein-ancient "0.6.10"]]}})
