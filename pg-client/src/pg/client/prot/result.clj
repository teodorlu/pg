(ns pg.client.prot.result)

(defprotocol IResult

  (handle [this messages])

  (get-connection [this])

  (add-RowDescription [this RowDescription])

  (add-DataRow [this DataRow])

  (add-ErrorResponse [this ErrorResponse])

  (add-CommandComplete [this CommandComplete])

  (complete [this])

  (to-clojure [this i]))
