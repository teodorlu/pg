(ns pg.client.proto.message)

(defprotocol IMessage
  (to-bb [this]))
