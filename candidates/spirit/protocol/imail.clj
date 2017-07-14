(ns spirit.protocol.imail)

(defprotocol IMailer
  (-send-mail [mailer email template data]))

(defprotocol IMailbox
  (-list-mail  [mailbox])
  (-clear-mail [mailbox]))
