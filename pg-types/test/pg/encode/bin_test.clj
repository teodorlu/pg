(ns pg.encode.bin-test
  (:import
   java.util.Date
   java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetTime
   java.time.ZoneOffset
   java.time.ZonedDateTime
   java.time.OffsetDateTime
   java.time.LocalDateTime
   java.math.BigDecimal
   java.math.BigInteger)
  (:require
   [pg.decode.bin :refer [decode]]
   [pg.bytes :as bytes]
   [clojure.string :as str]
   [pg.oid :as oid]
   [pg.encode.bin :refer [encode]]
   [clojure.test :refer [deftest is testing]]))


(deftest test-numbers

  ;; int

  (let [res (encode 1)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res)))

  (let [res (encode (int 1))]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (short 1))]
    (is (bytes/== (byte-array [0 1]) res)))

  ;; byte

  (let [res (encode (byte 1))]
    (is (bytes/== (byte-array [0 1]) res)))

  (let [res (encode (byte 1) oid/int4)]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (byte 1) oid/int8)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res)))

  ;; float

  (let [res (encode (float 1.1) oid/float4)]
    (is (bytes/== (byte-array [63, -116, -52, -51]) res)))

  (let [res (encode (double 1.1) oid/float8)]
    (is (bytes/== (byte-array [63, -15, -103, -103, -103, -103, -103, -102]) res))
    (is (= (double 1.1) (decode res oid/float8))))

  ;; int -> float

  (let [res (encode (short 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (int 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (long 1) oid/float4)]
    (is (bytes/== (byte-array [63, -128, 0, 0]) res)))

  (let [res (encode (short 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (encode (int 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (encode (long 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (encode (long 1) oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  ;; bigint

  (let [res (encode (bigint 1) oid/numeric)]
    (is (bytes/== (byte-array [0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0]) res)))

  (let [res (encode (bigint 1) oid/int2)]
    (is (bytes/== (byte-array [0 1]) res)))

  (let [res (encode (bigint 1) oid/int4)]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (bigint 1) oid/int8)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res)))

  ;; biginteger

  (let [res (encode (new BigInteger "1") oid/numeric)]
    (is (bytes/== (byte-array [0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0]) res)))

  (let [res (encode (new BigInteger "1") oid/int2)]
    (is (bytes/== (byte-array [0 1]) res)))

  (let [res (encode (new BigInteger "1") oid/int4)]
    (is (bytes/== (byte-array [0 0 0 1]) res)))

  (let [res (encode (new BigInteger "1") oid/int8)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) res))))

(deftest test-big-decimal

  (doseq [value ["0"
                 "1"
                 "-1"
                 "123.456789"
                 "1234.56789"
                 "12345.6789"
                 "12345678.9"
                 "1.23456789"
                 "0.123456789"
                 "0.000000000000000000000123456789"
                 "123123123123123.000000000000000000000009"
                 "-0.00000000000000000000000100500"
                 "-23523423623423236212463460.00000000000000000000000100500333"
                 "342e10"
                 "-123e-8"]]

    (let [x1 (bigdec value)
          buf (encode x1 oid/numeric)
          x2 (decode buf oid/numeric)]
      (is (= x1 x2))))

  (let [x1 (bigdec "1")
        buf (encode x1 oid/int2)]
    (is (bytes/== (byte-array [0 1]) buf)))

  (let [x1 (bigdec "1")
        buf (encode x1 oid/int4)]
    (is (bytes/== (byte-array [0 0 0 1]) buf)))

  (let [x1 (bigdec "1")
        buf (encode x1 oid/int8)]
    (is (bytes/== (byte-array [0 0 0 0 0 0 0 1]) buf)))

  (let [x1 (bigdec "1.1")
        buf (encode x1 oid/float4)]
    (is (bytes/== (byte-array [63, -116, -52, -51]) buf)))

  (let [x1 (bigdec "1.1")
        buf (encode x1 oid/float8)]
    (is (bytes/== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) buf))))


(deftest test-datetime

  ;; OffsetTime

  (let [val1 (OffsetTime/now)
        buf (encode val1 oid/timetz)
        val2 (decode buf oid/timetz)]
    (is (= val1 val2)))

  (let [val1 (OffsetTime/parse "12:28:23.336188+03:00")
        buf (encode val1 oid/time)
        val2 (decode buf oid/time)]
    (is (= (LocalTime/parse "12:28:23.336188") val2)))

  ;; LocalTime

  (let [val1 (LocalTime/now)
        buf (encode val1 oid/time)
        val2 (decode buf oid/time)]
    (is (= val1 val2)))

  (let [val1 (LocalTime/parse "12:41:00.005652")
        buf (encode val1 oid/timetz)
        val2 (decode buf oid/timetz)]
    (is (= (OffsetTime/parse "12:41:00.005652Z") val2)))

  ;; LocalDate

  (let [val1 (LocalDate/now)
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= val1 val2)))

  (let [val1 (LocalDate/parse "2022-01-01")
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2022-01-01T00:00") val2)))

  (let [val1 (LocalDate/parse "2022-01-01")
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2022-01-01T00:00Z") val2)))

  ;; OffsetDateTime

  (let [val1 (OffsetDateTime/now)
        buf (encode val1 oid/timestamptz)
        val2 ^OffsetDateTime (decode buf oid/timestamptz)]
    (is (instance? OffsetDateTime val2))
    (is (= (.atZoneSameInstant val1 ZoneOffset/UTC)
           (.atZoneSameInstant val2 ZoneOffset/UTC))))

  (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T09:44:20.611698") val2)))

  (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; ZonedDateTime

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-26T22:56:35.028508Z") val2)))

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-26T22:56:35.028508") val2)))

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= (LocalDate/parse "2023-07-26") val2)))

  ;; Instant

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-27T01:25:55.297834Z") val2)))

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T01:25:55.297834") val2)))

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; LocalDateTime

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-27T01:31:21.025913Z") val2)))

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T01:31:21.025913") val2)))

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; Date

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (encode val1 oid/date)
        val2 (decode buf oid/date)]
    (is (= (LocalDate/parse "2023-07-25") val2)))

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (encode val1 oid/timestamp)
        val2 (decode buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-25T01:00:00.123") val2)))

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (encode val1 oid/timestamptz)
        val2 (decode buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-25T01:00:00.123Z") val2))))


(deftest test-arrays

  ;; simple
  (let [val1 [1 2 3]
        buf (encode val1)
        val2 (decode buf oid/_int8)]
    (is (= [1 2 3] val2)))

  ;; multi-dim
  (let [val1 [[[1 nil 3] [4 nil 6]]
              [[3 nil 1] [9 nil 7]]]
        buf (encode val1)
        val2 (decode buf oid/_int8)]
    (is (= [[[1 nil 3] [4 nil 6]]
            [[3 nil 1] [9 nil 7]]]
           val2)))

  ;; string
  ;; bools
  ;; uuids
  ;; floats
  ;; dates
  ;; time
  ;; datetime
  ;; numeric





  )
