(defproject im.chit/spirit "0.5.3"
  :description "data. simplified"
  :url "https://www.github.com/zcaudate/spirit"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :aliases {"test" ["run" "-m" "hara.test" "exit"]}
  :dependencies [[org.clojure/clojure    "1.8.0"]
                 [im.chit/hara.common    "2.5.8"]
                 [im.chit/hara.component "2.5.8"]
                 [im.chit/hara.data      "2.5.8"]
                 [im.chit/hara.event     "2.5.8"]
                 [im.chit/hara.function  "2.5.8"]
                 [im.chit/hara.string    "2.5.8"]
                 [inflections "0.13.0"]
                 [clj-http-lite "0.3.0"]
                 [cheshire "5.7.1"]
                 [com.draines/postal     "2.0.2"]
                 [com.rabbitmq/amqp-client "4.1.1"]]
  :java-source-paths ["java"]
  :publish {:theme  "mormont"
            
            :template {:site   "spirit"
                       :author "Chris Zheng"
                       :email  "z@caudate.me"
                       :icon   "favicon"
                       :tracking-enabled "true"
                       :tracking "UA-31320512-2"}

            :files {"index"
                    {:input "test/documentation/home_spirit.clj"
                     :title "spirit"
                     :subtitle "data. simplified."}
                    "spirit-datomic"
                    {:input "test/documentation/spirit_datomic.clj"
                     :title "datomic"
                     :subtitle "spirit.datomic API Reference"}
                    "spirit-rabbitmq"
                    {:input "test/documentation/spirit_rabbitmq.clj"
                     :title "rabbitmq"
                     :subtitle "spirit.rabbitmq API Reference"}}}
  
  :distribute {:jars  :dependencies
               :files [{:type :clojure
                        :levels 1
                        :path "src"}]}
  
  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5561.50"]
                                  [com.amazonaws/aws-java-sdk-dynamodb "1.11.136"]
                                  [im.chit/lucid.publish "1.3.12"]
                                  [im.chit/lucid.unit    "1.3.12"]
                                  [im.chit/lucid.package "1.3.12"]
                                  [im.chit/lucid.package "1.3.12"]
                                  [im.chit/hara.test  "2.5.8"]
                                  [clj-time "0.13.0"]]
                   :plugins [[lein-ancient "0.6.10"]]}})
