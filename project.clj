(defproject im.chit/spirit "0.5.1"
  :description "simplify data connectivity"
  :url "https://www.github.com/zcaudate/spirit"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure    "1.7.0"]
                 [im.chit/hara.common    "2.5.2"]
                 [im.chit/hara.component "2.5.2"]
                 [im.chit/hara.data      "2.5.2"]
                 [im.chit/hara.event     "2.5.2"]
                 [im.chit/hara.function  "2.5.2"]
                 [im.chit/hara.string    "2.5.2"]
                 [inflections "0.9.14"]]
  
  :publish {:theme  "stark"
            
            :template {:site   "spirit"
                       :author "Chris Zheng"
                       :email  "z@caudate.me"
                       :icon   "favicon"
                       :tracking-enabled "true"
                       :tracking "UA-31320512-2"}
            
            :files {"index"
                    {:template "home.html"
                     :input "test/documentation/home_spirit.clj"
                     :title "spirit"
                     :subtitle "simplify data connectivity"}}}
  
  :distribute {:jars  :dependencies
               :files [{:type :clojure
                        :levels 1
                        :path "src"}]}
  
  :profiles {:dev {:plugins []
                   :dependencies [[com.datomic/datomic-free "0.9.5350" :exclusions [joda-time]]
                                  [im.chit/hara.test  "2.5.2"]
                                  [clj-time "0.11.0"]
                                  [me.raynes/fs "1.4.6"]
                                  [cheshire "5.2.0"]]}})
