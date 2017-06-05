(ns spirit.protocol.iqueue)

(defprotocol IQueue
  (-list-queues     [mq])
  (-add-queue       [mq name opts])
  (-delete-queue    [mq name])
  
  (-list-exchanges  [mq])
  (-add-exchange    [mq name opts])
  (-delete-exchange [mq name])

  (-list-bindings   [mq])
  (-bind-exchange   [mq source dest opts])
  (-bind-queue      [mq source dest opts])

  (-list-consumers  [mq])
  (-add-consumer    [mq name handler])
  (-delete-consumer [mq name id])

  (-publish         [mq exchange message opts]))