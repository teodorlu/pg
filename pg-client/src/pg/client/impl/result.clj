(ns pg.client.impl.result
  (:import
   java.util.List
   java.util.ArrayList
   java.util.Map
   java.util.HashMap)
  (:require
   [pg.client.prot.message :as message]
   [pg.client.prot.result :as result]
   [pg.decode.txt :as txt]))


(defn decode-row [RowDescription DataRow]

  (let [{:keys [columns]}
        RowDescription

        {:keys [values]}
        DataRow

        result
        (new ArrayList)]

    (map (fn [column value]

           (let [{:keys [name
                         format
                         type-oid]}
                 column]

             (case (int format)

               0
               (let [text
                     (new String ^bytes value "UTF-8")]
                 (txt/-decode type-oid text)))))
         columns
         values)))


(defn unify-fields [fields]

  (let [field->fields
        (group-by identity fields)

        len
        (count fields)]

    (loop [i 0
           result []
           field->idx {}]

      (if (= i len)
        result

        (let [field
              (get fields i)

              fields
              (get field->fields field)]

          (if (= 1 (count fields))

            (recur (inc i)
                   (conj result field)
                   field->idx)

            (let [idx
                  (get field->idx field 0)

                  field'
                  (format "%s_%s" field idx)

                  field->idx'
                  (assoc field->idx field (inc idx))]

              (recur (inc i)
                     (conj result field')
                     field->idx'))))))))


(defprotocol IFrame
  (add-RowDescription [this RowDescription])
  (add-CommandComplete [this CommandComplete])
  (add-DataRow [this DataRow])
  (complete [this]))


(deftype Frame
    [;; init
     ^Map  -opts
     ^List -rows
     ;; state
     ^Map  ^:unsynchronized-mutable -RowDescription
     ^Map  ^:unsynchronized-mutable -CommandComplete
     ^List ^:unsynchronized-mutable -fields]

  IFrame

  (add-RowDescription [this RowDescription]

    (set! -RowDescription RowDescription)

    (let [{:keys [fn-column]}
          -opts

          fields
          (->> RowDescription
               (:columns)
               (mapv :name)
               (unify-fields)
               (mapv fn-column))]

      (set! -fields fields)))

  (add-CommandComplete [this CommandComplete]
    (set! -CommandComplete CommandComplete))

  (add-DataRow [this DataRow]

    (let [values
          (decode-row -RowDescription DataRow)

          {:keys [column-count]}
          -RowDescription

          {:keys [as-vectors?
                  as-maps?
                  as-java-maps?]}
          -opts

          row
          (cond

            as-maps?
            (zipmap -fields values)

            as-vectors?
            values

            as-java-maps?
            (doto (new HashMap)
              (.putAll (zipmap -fields values)))

            :else
            (zipmap -fields values))]

      (conj! -rows row)))

  (complete [this]
    (persistent! -rows)))


(defn make-frame [opt]
  (new Frame opt (transient []) nil nil nil))


(defn afirst [^List a]
  (when (-> a .size (> 0))
    (.get a 0)))


(deftype Result
    [connection
     ^Map  -opts
     ^Frame ^:unsynchronized-mutable -frame
     ^List -frames
     ^List -list-ErrorResponse]

  result/IResult

  (handle [this messages]
    (result/complete
     (reduce
      (fn [result message]
        (message/handle message result connection))
      this
      messages)))

  (get-connection [this]
    connection)

  (add-RowDescription [this RowDescription]
    (add-RowDescription -frame RowDescription))

  (add-DataRow [this DataRow]
    (add-DataRow -frame DataRow))

  (add-ErrorResponse [this ErrorResponse]
    (.add -list-ErrorResponse ErrorResponse))

  (add-CommandComplete [this CommandComplete]
    (add-CommandComplete -frame CommandComplete)
    (.add -frames -frame)
    (set! -frame (make-frame -opts)))

  (complete [this]

    (let [er (afirst -list-ErrorResponse)]

      (cond

        er
        (throw (ex-info "ErrorResponse" er))

        (= (.size -frames) 1)
        (-> -frames afirst complete)

        (> (.size -frames) 1)
        (mapv complete -frames)))))


(def opt-default
  {:fn-column keyword})


(defn make-result
  ([connection]
   (make-result connection nil))

  ([connection opt]

   (let [opt
         (merge opt-default opt)]

     (new Result
          connection
          opt
          (make-frame opt)
          (new ArrayList)
          (new ArrayList)))))
