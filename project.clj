(defproject im.chit/spirit "0.5.1"
  :description "data. simplified"
  :url "https://www.github.com/zcaudate/spirit"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :aliases {"test" ["run" "-m" "hara.test" "exit"]}
  :dependencies [[org.clojure/clojure    "1.8.0"]
                 [im.chit/hara.common    "2.5.6"]
                 [im.chit/hara.component "2.5.6"]
                 [im.chit/hara.data      "2.5.6"]
                 [im.chit/hara.event     "2.5.6"]
                 [im.chit/hara.function  "2.5.6"]
                 [im.chit/hara.string    "2.5.6"]
                 [inflections "0.9.14"]]
  
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
                    "datomic"
                    {:input "test/documentation/spirit_datomic.clj"
                     :title "datomic"
                     :subtitle "API Reference"}}}
  
  :distribute {:jars  :dependencies
               :files [{:type :clojure
                        :levels 1
                        :path "src"}]}
  
  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.5561"]
                                  ;[com.amazonaws/aws-java-sdk-dynamodb "1.11.136"]
                                  ;[org.clojure/java.jdbc "0.7.0-alpha3"]
                                  ;[org.postgresql/postgresql "42.1.1"]
                                  [im.chit/lucid.publish "1.3.10"]
                                  [im.chit/lucid.unit    "1.3.10"]
                                  [im.chit/lucid.package "1.3.10"]
                                  [im.chit/lucid.package "1.3.10"]
                                  [im.chit/hara.test  "2.5.6"]
                                  [clj-time "0.11.0"]
                                  [me.raynes/fs "1.4.6"]
                                  [cheshire "5.2.0"]]}})
