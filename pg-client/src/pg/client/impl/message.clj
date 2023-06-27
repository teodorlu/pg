(ns pg.client.impl.message
  (:import
   clojure.lang.Keyword
   java.nio.ByteBuffer
   java.util.ArrayList
   java.util.List
   java.util.Map
   java.util.Set)
  (:require
   [pg.bytes.array :as array]
   [pg.client.bb :as bb]
   [pg.client.coll :as coll]
   [pg.client.bytes :as bytes]
   [pg.client.codec :as codec]
   [pg.client.prot.connection :as connection]
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as result]
   [pg.encode.txt :as txt]))


(defn parse-token ^Keyword [^Byte b]
  (case (char b)
    \S :severity
    \V :verbosity
    \C :code
    \M :message
    \D :detail
    \H :hint
    \P :position
    \p :position-internal
    \q :query
    \W :stacktrace
    \s :schema
    \t :table
    \c :column
    \d :datatype
    \n :constraint
    \F :file
    \L :line
    \R :function
    (-> b char str keyword)))


(defrecord NegotiateProtocolVersion
    [^Integer version
     ^List params]

  message/IMessage

  (handle [this result connection]
    result)

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          version
          (bb/read-int32 bb)

          param-count
          (bb/read-int32 bb)

          params
          (coll/doN [_ param-count]
            (bb/read-cstring bb encoding))]

      (assoc this
             :version version
             :params params))))


(defmethod message/tag->message \v [_]
  (new NegotiateProtocolVersion nil nil))


(defrecord NoticeResponse
    [^Map fields]

  message/IMessage

  (handle [this result connection]
    (connection/handle-notice connection fields)
    result)

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          fields
          (loop [acc {}]
            (let [b (bb/read-byte bb)]
              (if (zero? b)
                acc
                (let [token
                      (parse-token b)
                      field
                      (bb/read-cstring bb encoding)]
                  (recur (assoc acc token field))))))]

      (assoc this :fields fields))))


(defmethod message/tag->message \N [_]
  (new NoticeResponse nil))


(defn bb-encode ^ByteBuffer [^String encoding tag parts]

  (let [len-payload
        (reduce
         (fn [result part]
           (cond

             (bytes/byte? part)
             (inc result)

             (bytes? part)
             (+ result (alength ^bytes part))

             (string? part)
             (+ result (alength (.getBytes ^String part encoding)) 1)

             :else
             (throw (ex-info "Wrong part type" {:tag tag :part part}))))
         0
         parts)

        bb
        (bb/allocate (+ (if tag 1 0) 4 len-payload))]

    (when tag
      (bb/write-byte bb tag))

    (bb/write-int32 bb (+ 4 len-payload))

    (doseq [part parts]
      (cond

        (bytes/byte? part)
        (bb/write-byte bb part)

        (bytes? part)
        (bb/write-bytes bb part)

        (string? part)
        (bb/write-cstring bb part encoding)))

    (bb/rewind bb)
    bb))


(defrecord Sync []

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \S nil))))


(defrecord Flush []

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \H nil))))


(defrecord StartupMessage
    [^Integer protocol-version
     ^String user
     ^String database
     ^Map options]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 nil
                 (-> [(array/arr32 protocol-version)
                      "user"
                      user
                      "database"
                      database]
                     (into (mapcat identity options))
                     (conj (byte 0)))))))


(defrecord AuthenticationOk
    [^Integer status]

  message/IMessage

  (handle [this result connection]
    result))


(defrecord AuthenticationKerberosV5
    [^Integer status])


(defrecord AuthenticationSCMCredential
    [^Integer status])


(defrecord AuthenticationGSS
    [^Integer status])


(defrecord AuthenticationGSSContinue
    [^Integer status
     ^bytes auth])


(defrecord AuthenticationSSPI
    [^Integer status])


(defrecord AuthenticationSASL
    [^Integer status
     ^Set sasl-types])


(defrecord AuthenticationSASLContinue
    [^Integer status
     ^String server-first-message])


(defrecord AuthenticationSASLFinal
    [^Integer status
     ^String server-final-message])


(defmethod message/status->message 0
  [status bb connection]
  (new AuthenticationOk status))


(defmethod message/status->message 2
  [status bb connection]
  (new AuthenticationKerberosV5 status))


(defmethod message/status->message 6
  [status bb connection]
  (new AuthenticationSCMCredential status))


(defmethod message/status->message 7
  [status bb connection]
  (new AuthenticationGSS status))


(defrecord AuthenticationResponse
    [^Integer status]

  message/IMessage

  (from-bb [this bb connection]

    (let [status (bb/read-int32 bb)]
      (message/status->message status bb connection))))


(defmethod message/tag->message \R [_]
  (new AuthenticationResponse nil))


(defrecord ReadyForQuery
    [^Character tx-status]

  message/IMessage

  (handle [this result connection]
    (connection/set-tx-status connection tx-status)
    result)

  (from-bb [this bb connection]
    (let [tx-status
          (-> bb bb/read-byte char str keyword)]
      (assoc this :tx-status tx-status))))

(defmethod message/tag->message \Z [_]
  (new ReadyForQuery nil))


(defrecord BackendKeyData
    [^Integer pid
     ^Integer secret-key]

  message/IMessage

  (handle [this result connection]
    (connection/set-pid connection pid)
    (connection/set-secret-key connection secret-key)
    result)

  (from-bb [this bb connection]

    (let [pid
          (bb/read-int32 bb)

          secret-key
          (bb/read-int32 bb)]

      (assoc this
             :pid pid
             :secret-key secret-key))))


(defmethod message/tag->message \K [_]
  (new BackendKeyData nil nil))


(defrecord Parse
    [^String statement-name
     ^String query
     ^List param-oids]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 \P
                 (-> [statement-name
                      query
                      (array/arr16 (count param-oids))]
                     (into (mapv array/arr32 param-oids)))))))


(defrecord Close
    [^Character source-type
     ^String source]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 \C
                 [(byte source-type)
                  source]))))


(defrecord CloseComplete []

  message/IMessage

  (handle [this result connection]
    result)

  (from-bb [this bb connection]
    this))


(defmethod message/tag->message \3 [_]
  (new CloseComplete))


(defrecord Describe
    [^Character source-type
     ^String source]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 \D
                 [(byte source-type)
                  source]))))


(defrecord ParameterDescription
    [^Short param-count
     ^List param-oids]

  message/IMessage

  (handle [this result connection]
    result)

  (from-bb [this bb connection]

    (let [param-count
          (bb/read-int16 bb)

          param-oids
          (coll/doN [_ param-count]
            (bb/read-int32 bb))]

      (assoc this
             :param-count param-count
             :param-oids param-oids))))


(defmethod message/tag->message \t [_]
  (new ParameterDescription nil nil))


(defrecord ParseComplete []

  message/IMessage

  (handle [this result connection]
    result)

  (from-bb [this bb connection]
    this))


(defmethod message/tag->message \1 [_]
  (new ParseComplete))


(defrecord Bind
    [^String portal-name
     ^String statement-name
     ^List format-params
     ^List params
     ^List format-columns]

  message/IMessage

  (to-bb [this connection]

    (let [encoding
          (connection/get-client-encoding connection)

          parts
          (new ArrayList)]

      (doto parts
        (.add portal-name)
        (.add statement-name)
        (.add (array/arr16 (count format-params)))
        (.addAll (mapv array/arr16 format-params))
        (.add (array/arr16 (count params))))

      (doseq [param params]

        (if (nil? param)
          (.add parts (array/arr32 -1))

          (let [encoded
                (txt/encode param nil nil)

                buf
                (.getBytes encoded encoding)

                len
                (count buf)]

            (.add parts (array/arr32 len))
            (.add parts buf))))

      (doto parts
        (.add (array/arr16 (count format-columns)))
        (.addAll (mapv array/arr16 format-columns)))

      (bb-encode encoding \B (vec parts)))))


(defrecord BindComplete []

  message/IMessage

  (handle [this result connection]
    result)

  (from-bb [this bb connection]
    this))


(defmethod message/tag->message \2 [_]
  (new BindComplete))


(defrecord Execute
    [^String portal-name
     ^Integer row-count]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding
                 \E
                 [portal-name
                  (array/arr32 row-count)]))))


(defrecord RowColumn
    [^Integer index
     ^String  name
     ^Integer table-oid
     ^Short   column-oid
     ^Integer type-oid
     ^Short   type-len
     ^Integer type-mod
     ^Short   format])


(defrecord RowDescription
    [^Integer column-count
     ^List columns]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-RowDescription this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          column-count
          (bb/read-int16 bb)

          columns
          (coll/doN [i column-count]
            (new RowColumn
                 i
                 (bb/read-cstring bb encoding)
                 (bb/read-int32 bb)
                 (bb/read-int16 bb)
                 (bb/read-int32 bb)
                 (bb/read-int16 bb)
                 (bb/read-int32 bb)
                 (bb/read-int16 bb)))]

      (assoc this
             :column-count column-count
             :columns columns))))


(defmethod message/tag->message \T [_]
  (new RowDescription nil nil))


(defrecord ParameterStatus
    [^String param
     ^Object value]

  message/IMessage

  (handle [this result connection]
    (connection/set-parameter connection param value)
    result)

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          param
          (-> bb (bb/read-cstring encoding))

          value
          (-> bb (bb/read-cstring encoding))]

      (assoc this
             :param param
             :value value))))


(defmethod message/tag->message \S [_]
  (new ParameterStatus nil nil))


(defrecord ErrorResponse
    [^Map errors]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-ErrorResponse this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          errors
          (loop [acc {}]
            (let [b (bb/read-byte bb)]
              (if (zero? b)
                acc
                (let [token
                      (parse-token b)
                      field
                      (bb/read-cstring bb encoding)]
                  (recur (assoc acc token field))))))]

      (assoc this :errors errors))))


(defmethod message/tag->message \E [_]
  (new ErrorResponse nil))


(defrecord EmptyQueryResponse
    []

  message/IMessage

  (from-bb [this bb connection]
    this)

  (handle [this result connection]
    result))


(defmethod message/tag->message \I [_]
  (new EmptyQueryResponse))


(defrecord DataRow
    [^List values]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-DataRow this)))

  (from-bb [this bb connection]

    (let [value-count
          (bb/read-int16 bb)

          values
          (coll/doN [i value-count]
            (let [len (bb/read-int32 bb)]
              (when-not (= len -1)
                (bb/read-bytes bb len))))]

      (assoc this :values values))))


(defmethod message/tag->message \D [_]
  (new DataRow nil))


(defrecord NoData [])


(defrecord CommandComplete
    [^String tag]

  message/IMessage

  (handle [this result connection]
    (doto result
      (result/add-CommandComplete this)))

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          tag
          (bb/read-cstring bb encoding)]

      (assoc this :tag tag))))


(defmethod message/tag->message \C [_]
  (new CommandComplete nil))


(defrecord PasswordMessage
    [^String password]

  message/IMessage

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \p [password]))))


(defrecord Query
    [^String query]

  message/IMessage

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          tag
          (bb/read-cstring bb encoding)]

      (assoc this :tag tag)))

  (to-bb [this connection]
    (let [encoding
          (connection/get-client-encoding connection)]
      (bb-encode encoding \Q [query]))))


(defrecord Terminate []

  message/IMessage

  (to-bb [this connection]
    (bb-encode nil \X nil)))


(defrecord NotificationResponse
    [^Integer pid
     ^String channel
     ^String message]

  message/IMessage

  (from-bb [this bb connection]

    (let [encoding
          (connection/get-server-encoding connection)

          pid
          (bb/read-int32 bb)

          channel
          (bb/read-cstring bb encoding)

          message
          (bb/read-cstring bb encoding)]

      (assoc this
             :pid pid
             :channel channel
             :message message)))

  (handle [this result connection]
    (connection/handle-notification connection this)
    result))


(defmethod message/tag->message \A [_]
  (new NotificationResponse nil nil nil))
