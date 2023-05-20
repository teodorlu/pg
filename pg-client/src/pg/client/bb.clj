(ns pg.client.bb
  (:import
   java.nio.channels.SocketChannel
   java.io.ByteArrayOutputStream
   java.nio.ByteBuffer))


(defmacro remaining [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (remaining)))


(defmacro rewind [bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (rewind)))


(defn read-int16 [^ByteBuffer bb]
  (.getShort bb))


(defmacro allocate [size]
  `(ByteBuffer/allocate ~size))


(defn write-int32 [^ByteBuffer bb value]
  (.putInt bb value))


(defn write-byte [^ByteBuffer bb value]
  (.put bb (byte value)))


(defn write-bytes [^ByteBuffer bb ^bytes buf]
  (.put bb buf))


(defn write-cstring
  [^ByteBuffer bb ^String string ^String encoding]
  (doto bb
    (write-bytes (.getBytes string encoding))
    (write-byte 0)))


(defn read-byte [^ByteBuffer bb]
  (.get bb))


(defn read-bytes ^bytes [^ByteBuffer bb len]
  (let [buf (byte-array len)]
    (.get bb buf)
    buf))


(defn read-cstring ^String [^ByteBuffer bb ^String encoding]
  (let [out (new ByteArrayOutputStream)]
    (loop []
      (let [b (.get bb)]
        (if (zero? b)
          (.toString out encoding)
          (do
            (.write out b)
            (recur)))))))


(defmacro read-int32 [^ByteBuffer bb]
  `(.. ~(with-meta bb {:tag `ByteBuffer}) (getInt)))


(defn debug [^ByteBuffer bb]
  (println (vec (.array bb))))


(defn read-from [^SocketChannel ch ^ByteBuffer bb]
  (while (not (zero? (remaining bb)))
    (.read ch bb)))


(defn write-to [^SocketChannel ch ^ByteBuffer bb]
  (let [written
        (.write ch (rewind bb))

        remaining
        (remaining bb)]

    (when-not (zero? remaining)
      (throw (ex-info "Incomplete `write-to` operation"
                      {:written written
                       :remaining remaining})))))
