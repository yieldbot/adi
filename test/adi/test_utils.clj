(ns adi.test-utils
  (:use midje.sweet)
  (:require [adi.utils :as ut]))


(fact "dissoc-in"
  (ut/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (ut/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (ut/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (ut/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {})

(fact "dissoc-in-keepempty"
 (ut/dissoc-in-keepempty {:a 2 :b 2} [:a]) => {:b 2}
 (ut/dissoc-in-keepempty {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
 (ut/dissoc-in-keepempty {:a {:c 3}} [:a :c]) => {:a {}}
 (ut/dissoc-in-keepempty {:a {:b {:c 3}}} [:a :b :c]) => {:a {:b {}}})

(fact "?sym generates a unique symbol that starts with ?"
  (ut/?sym) => (every-checker
                    symbol?
                    (fn [x] (= \? (first (name x))))
                    (fn [x] (not= (ut/?sym) x))))

(fact "no-repeats outputs a filtered list of values"
  (ut/no-repeats [1 1 2 2 3 3 4 5 6]) => [1 2 3 4 5 6]
  (ut/no-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}]) => [{:n 1} {:n 2}]
  (ut/no-repeats even? [2 4 6 1 3 5]) => [2 1])

(fact "k-str returns the string representation with the colon"
  (ut/k-str nil) => ""
  (ut/k-str :hello) => "hello"
  (ut/k-str :hello/there) => "hello/there"
  (ut/k-str :hello/there/man) => "hello/there/man")

(fact "k-merge merges a sequence of keys together to form a keyword"
  (ut/k-merge []) => nil
  (ut/k-merge [:hello]) => :hello
  (ut/k-merge [:hello :there]) => :hello/there
  (ut/k-merge [:a :b :c :d]) => :a/b/c/d)

(fact "k-unmerge does the inverse of k-merge.
       It takes a keyword and turns it into a vector"
  (ut/k-unmerge nil) => []
  (ut/k-unmerge :hello) => [:hello]
  (ut/k-unmerge :hello/there) => [:hello :there]
  (ut/k-unmerge :a/b/c/d) => [:a :b :c :d])

(fact "k-nsv will output the namespace in vector form"
  (ut/k-nsv nil) => []
  (ut/k-nsv :hello) => []
  (ut/k-nsv :hello/there) => [:hello]
  (ut/k-nsv :hello/there/again) => [:hello :there])

(fact "k-nsv? will check is the output of key namespace is the that specified"
  (ut/k-nsv? nil []) => true
  (ut/k-nsv? :hello []) => true
  (ut/k-nsv? :hello/there [:hello]) => true
  (ut/k-nsv? :hello/there/again [:hello :there]) => true
  (ut/k-nsv? :hello/there/again [:hello]) => false)

(fact "k-ns will output the namespace of a key"
  (ut/k-ns nil) => nil
  (ut/k-ns :hello) => nil
  (ut/k-ns :hello/there) => :hello
  (ut/k-ns :hello/there/again) => :hello/there)

(fact "k-ns? will check if the key contains a namespace"
  (ut/k-ns? nil) => false
  (ut/k-ns? nil nil) => true
  (ut/k-ns? :hello) => false
  (ut/k-ns? :hello nil) => true
  (ut/k-ns? :hello/there) => true
  (ut/k-ns? :hello/there :hello) => true
  (ut/k-ns? :hello/there/again) => true
  (ut/k-ns? :hello/there/again :hello/there) => true)

(fact "k-z will output the key"
  (ut/k-z nil) => nil
  (ut/k-z :hello) => :hello
  (ut/k-z :hello/there) => :there
  (ut/k-z :hello/there/again) => :again)

(fact "k-z will check if the output is that specified"
  (ut/k-z? nil nil) => true
  (ut/k-z? :hello :hello) => true
  (ut/k-z? :hello/there :there) => true
  (ut/k-z? :hello/there/again :again) => true)


(fact "list-ns will output all unique kns namespace"
  (ut/list-ns {:hello/a 1 :hello/b 2 :a 3 :b 4}) => #{nil :hello}
  (ut/list-ns {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4}) => #{:hello :there})

(fact "list-ks will output all keys"
  (ut/list-ks {:hello/a 1 :hello/b 2
               :there/a 3 :there/b 4} :hello)
  => #{:hello/a :hello/b})

(fact "list-all-keys will output all keys"
  (ut/list-all-keys 
    {:a {:b 1 :c 2}})
  => #{:a :b :c})

(fact "flatten-all-keys will take a map of maps and make it into a single map"
  (ut/flatten-all-keys {}) => {}
  (ut/flatten-all-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/flatten-all-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (ut/flatten-all-keys {:a {:b {:c 3 :d 4}
                        :e {:f 5 :g 6}}
                    :h {:i 7} })
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})

(fact "flatten-keys will take a map of maps and make it into a single map"
  (ut/flatten-keys {}) => {}
  (ut/flatten-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/flatten-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (ut/flatten-keys {:a {:b {:c 3 :d 4}
                             :e {:f 5 :g 6}}
                         :h {:i 7} })
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7})

(fact "treeify-keys will take a single map of compound keys and make it into a tree"
  (ut/treeify-keys {}) => {}
  (ut/treeify-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/treeify-keys {:a/b 2 :a/c 3}) => {:a {:b 2 :c 3}}
  (ut/treeify-keys {:a {:b/c 2} :a/d 3}) => {:a {:b/c 2 :d 3}}
  (ut/treeify-keys {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})
  => {:a {:b {:c 3 :d 4}
          :e {:f 5 :g 6}}
      :h {:i 7}})

(fact "treeify-keys will take a map of nested compound keys and shape it into a non-compound tree"
   (ut/treeify-all-keys nil) => {}
   (ut/treeify-all-keys {}) => {}
   (ut/treeify-all-keys {:a 1 :b 3}) => {:a 1 :b 3}
   (ut/treeify-all-keys {:a/b 2 :a {:c 3}}) => {:a {:b 2 :c 3}}
   (ut/treeify-all-keys {:a {:b/c 2} :a/d 3}) => {:a {:b {:c 2} :d 3}})


(fact "extend-keys will extend a treeified map with given namespace keys"
  (ut/extend-keys {:a 1 :b 2} [:hello] [])
  => {:hello {:a 1 :b 2}}

  (ut/extend-keys {:a 1 :b 2} [:hello :there] [])
  => {:hello {:there {:a 1 :b 2}}}

  (ut/extend-keys {:a 1 :b 2} [:hello] [:a])
  => {:hello {:b 2} :a 1}

  (ut/extend-keys {:a 1 :b 2} [:hello] [:a :b])
  => {:hello {} :a 1 :b 2})


(fact "contract-keys will make a treefied map"
  (ut/contract-keys {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}
  
  (ut/contract-keys {:hello {:a 1 :b 2}
                     :there {:a 3 :b 4}} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (ut/contract-keys {:hello/there/a 1
                     :hello/there/b 2
                     :again/there/a 3
                     :again/there/b 4} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}


  (ut/contract-keys {:hello {:there {:a 1 :b 2}}
                     :again {:there {:a 3 :b 4}}} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}

  (ut/contract-keys {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [:+])
  => {:a 1 :b 2 :+ {:there {:a 3 :b 4}}})
