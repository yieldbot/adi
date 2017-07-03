(ns spirit.common.mail
  (:require [spirit.protocol.imail :as mail]
            [spirit.common.atom :as atom]
            [hara.component :as component]
            [hara.string.mustache :as mustache]))

(defn send-mail
  "sends mail to the following mailbox
 
   (-> (doto  (mail {:type :mock
                     :file \"test.mailbox.db\"})
         (send-mail \"z@caudate.me\" nil {:a 1 :b 2})
         (send-mail \"z@caudate.me\" nil {:x 1 :y 2})
         (send-mail \"z@caudate.me\" nil {:a 1 :b 2}))
       (list-mail))
  => {\"z@caudate.me\" [{:a 1, :b 2}
                       {:x 1, :y 2}
                       {:a 1, :b 2}]}"
  {:added "0.5"}
  [mailer email template data]
  (mail/-send-mail mailer email template data))

(defn list-mail
  "lists all mail sent to the mailbox
   
   (-> (doto (mail {:type :mock})
         (send-mail \"z@caudate.me\" nil {:a 1 :b 2}))
       (list-mail))
   => {\"z@caudate.me\" [{:a 1, :b 2}]}"
  {:added "0.5"}
  [mailbox]
  (mail/-list-mail mailbox))

(defn clear-mail
  "clears all mail sent to the mailbox
 
   (-> (doto (mail {:type :mock})
         (send-mail \"z@caudate.me\" nil {:a 1 :b 2})
         (clear-mail))
       (list-mail))
   => {}"
  {:added "0.5"}
  [mailbox]
  (mail/-clear-mail mailbox))

(extend-type clojure.lang.Atom

  mail/IMailbox
  (-clear-mail [atom]
    (swap! atom update-in [:mailbox] empty)
    atom)

  (-list-mail [atom]
    (:mailbox @atom))

  mail/IMailer
  (-send-mail [atom email template data]
    (swap! atom update-in [:mailbox email]
           (fnil #(conj % data) []))
    atom))

(defrecord MockMailer [state]
  
  Object
  (toString [mailer]
    (str "#mock.mail " (reduce-kv  (fn [out k v]
                                     (assoc out k (count (seq v))))
                                   {}
                                   (list-mail state))))
  
  mail/IMailbox
  (-clear-mail [mailbox]
    (clear-mail state)
    mailbox)

  (-list-mail [mailbox]
    (list-mail state))

  mail/IMailer
  (-send-mail [mailer email template data]
    (send-mail state email template data)
    mailer))

(defmethod print-method MockMailer
  [v w]
  (.write w (str v)))

(defmulti create
  "creates a mail service
 
   (create {:type :raw})
 
   (create {:type :mock
            :file \"test.mail.db\"})"
  {:added "0.5"}
  :type)

(defmethod create :raw
  [m]
  (atom {:mailbox  {}
         :settings m}))

(defmethod create :mock
  [{:keys [file format] :as m}]
  (let [state  (create (assoc m :type :raw))
        cursor (cond-> (atom/cursor state [:mailbox])
                 file
                 (atom/file-out m))]
    (map->MockMailer {:state state})))

(defn mail
  "creates a mail service
   
   (mail {:type :postal})"
  {:added "0.5"}
  [m]
  (-> (create m)
      (component/start)))
