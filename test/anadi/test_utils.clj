(ns anadi.test-utils
  (:use midje.sweet)
  (:require [anadi.utils :as ut]))

(fact "kname returns the string representation with the colon"
  (ut/key-str nil) => ""
  (ut/key-str :hello) => "hello"
  (ut/key-str :hello/there) => "hello/there")

(fact "merge-keys merges a sequence of keys together to form a keyword"
  (ut/merge-keys []) => nil
  (ut/merge-keys [:hello :there]) => :hello/there
  (ut/merge-keys [:a :b :c :d]) => :a/b/c/d)

(fact "seperate-keys does the inverse of merge-keys.
       It takes a keyword and turns it into a vector"
  (ut/seperate-keys :hello/there) => [:hello :there]
  (ut/seperate-keys :a/b/c/d) => [:a :b :c :d])

(fact "keys-ns? will check if the key contains a namespace"
  (ut/key-ns? nil) => false
  (ut/key-ns? :hello) => false
  (ut/key-ns? :hello/there) => true
  (ut/key-ns? :hello/there/again) => true)

(fact "keys-ns will output the key namespace"
  (ut/key-ns nil) => nil
  (ut/key-ns :hello) => nil
  (ut/key-ns :hello/there) => :hello
  (ut/key-ns :hello/there/again) => :hello/there)

(fact "keys-name vill output the key name"
  (ut/key-name nil) => nil
  (ut/key-name :hello) => :hello
  (ut/key-name :hello/there) => :there
  (ut/key-name :hello/there/again) => :again)

(fact "flatten-keys will take a map of maps and make it into a single map"
  (ut/flatten-keys {}) => {}
  (ut/flatten-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/flatten-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (ut/flatten-keys {:a {:b {:c 3 :d 4}
                        :e {:f 5 :g 6}}
                    :h {:i 7} })
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})

(fact "treeify-keys will take a single map of compound keys and make it into a tree"
  (ut/treeify-keys {}) => {}
  (ut/treeify-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/treeify-keys {:a/b 2 :a/c 3}) => {:a {:b 2 :c 3}}
  (ut/treeify-keys {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})
  => {:a {:b {:c 3 :d 4}
          :e {:f 5 :g 6}}
      :h {:i 7}})


(fact "extend-key-ns will extend a map with given namespace keys"
  (ut/extend-key-ns {:a 1 :b 2} [:hello] [])
  => {:hello {:a 1 :b 2}}

  (ut/extend-key-ns {:a 1 :b 2} [:hello] [:a])
  => {:hello {:b 2} :a 1}

  (ut/extend-key-ns {:a 1 :b 2} [:hello] [:a :b])
  => {:hello {} :a 1 :b 2})


(fact "contract-key-ns will make a treefied map only"
  (ut/contract-key-ns {:hello/a 1
                       :hello/b 2
                       :there/a 3
                       :there/b 4} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (ut/contract-key-ns {:hello/a 1
                       :hello/b 2
                       :there/a 3
                       :there/b 4} [:hello] [:+])
  => {:a 1 :b 2 :+ {:there {:a 3 :b 4}}})
