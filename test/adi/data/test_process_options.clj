(ns adi.data.test-process-options
  (:use adi.data
        adi.schema
        adi.utils
        midje.sweet))


(def o1-geni
  (add-idents {:account {:name [{:type :string :default "dave"}]}}))

(fact "process with default? option varied"
    (process {} o1-geni {})
  => {:# {:nss #{}}}

  (process {:account {}} o1-geni {})
  => {:account/name "dave", :# {:nss #{:account}}}

  (process {:account {}} o1-geni {:defaults? false})
  => {:# {:nss #{:account}}})


(def o2-geni
  (add-idents {:account {:name [{:type :string :restrict #{"dave"}}]}}))

(fact "process with restrict? option"
  (process {:account {:name "dave"}} o2-geni {})
  => {:account/name "dave", :# {:nss #{:account}}}

  (process {:account {:name "chris"}} o2-geni {})
  => (throws Exception)

  (process {:account {:name "chris"}} o2-geni {:restrict? false})
  => {:account/name "chris", :# {:nss #{:account}}}

  (process {:account {:name "chris"}} o2-geni {:restrict? true})
  => (throws Exception))

(def o3-geni
  (add-idents {:account {:name [{:type :string :required true}]}}))

(fact "process with required? option"
  (process {:account {:name "chris"}} o3-geni {})
  => {:account/name "chris" :# {:nss #{:account}}}

  (process {:account {}} o3-geni {})
  => (throws Exception)

  (process {:account {:name "chris"}} o3-geni {:required? false})
  => {:account/name "chris" :# {:nss #{:account}}}

    (process {:account {}} o3-geni {:required? false})
  => {:# {:nss #{:account}}}

  (process {:account {:name "chris"}} o3-geni {:required? true})
  => { :account/name "chris" :# {:nss #{:account}}}) ()


(def o4-geni
  (add-idents {:account {:name [{:type :string}]}}))

(fact "process with extras? option"
  (process {:account {:not-needed "chris"}} o4-geni {:extras? true})
  => {:# {:nss #{:account}}}

  (process {:account {:not-needed "chris"}} o4-geni {})
  => (throws Exception)

  (process {} o4-geni {})
  => {:# {:nss #{}}}

  (process {} o4-geni {:extras? true})
  => {:# {:nss #{}}})


(def o5-geni
  (add-idents {:account {:name [{:type :string}]}}))

(fact "process with extras? option"
  (process {:account {:name "chris"}} o5-geni {:sets-only? true})
  => {:account/name #{"chris"} :# {:nss #{:account}}}

  (process {} o5-geni {:sets-only? true})
  => {:# {:nss #{}}})
