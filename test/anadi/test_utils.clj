(ns anadi.test-utils
  (:use midje.sweet)
  (:require [anadi.utils :as ut]))

(fact "kname returns the string representation with the colon"
  (ut/kname nil) => ""
  (ut/kname :hello) => "hello"
  (ut/kname :hello/there) => "hello/there")

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
