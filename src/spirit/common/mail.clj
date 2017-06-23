(ns spirit.common.mail
  (:require [spirit.protocol.imail :as mail]
            [spirit.common.atom :as atom]
            [spirit.common.mustache :as mustache]))

(extend-type clojure.lang.Atom

  mail/IMailbox
  (-clear-mail [atom]
    (swap! atom update-in [:mailbox] empty))

  (-list-mail [atom]
    (:mailbox @atom))

  mail/IMailer
  #_(-send-mail [atom]
    (swap! atom update-in [:mailbox mail]
           (fnil #(conj % data) []))))


(defrecord MockMailer [state]

  Object
  (toString [mailer]
    (str "#mailer" (reduce-kv  (fn [out k v]
                                 (assoc out k (count (seq v))))
                               {}
                               (mail/-list-mail state))))

  mail/IMailbox
  (-clear-mail [mailer]
    (mail/-clear-mail state))

  (-list-mail [mailer]
    (mail/-list-mail state))

  mail/IMailer
  
  )

(defmethod print-method MockMailer
  [v w]
  (.write w (str v)))

(defmulti mailer :type)

(defmethod mailer :atom
  [m]
  (atom {:mailbox []
         :settings m}))

(defmethod mailer :mock
  [{:keys [file format] :as m
    :or {file "mailer.db"
         format :edn}}]
  (let [state (mailer (assoc m :type :atom))
        cursor (atom/file-backed (atom/cursor state [:mailbox])
                                 {:file file
                                  :initial {}})]
    (map->MockMailer {:state state})))

(comment

  
   (mustache/render "{{user.name}}" {:user {:name "hello"}})
  
  (mailer {:type :mock})
  (swap! (atom/file-backed "mailer.db")
         update-in [:a] conj :eue)
  
  {:type :atom
   :defaults {:from "z@caudate.me"}}
  
  (defn mailer
    ([]
     (MockMailer. (atom {})))
    ([m]
     (MockMailer. (atom {}))))

  (->  (mailer {}) 
       (interface/-send
        "z@caudate.me"
        nil
        {:a 1 :b 2})
       (interface/-send
        "z@caudate.me"
        nil
        {:a 1 :b 2})
       (get-mail "z@caudate.me"))
  )
