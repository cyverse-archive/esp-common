(ns esp-common.mock-event-db
  (:use esp-common.storage
        clojure-commons.error-codes
        [slingshot.slingshot :only [try+ throw+]]))

(def es-coll (atom {}))
(def e-coll (atom {}))

(defn uuid [] (string/upper-case (str (java.util.UUID/randomUUID))))

(defn failure
  [status msg]
  (throw+ {:error_code ERR_REQUEST_FAILED
          :status status
          :body msg}))

(defn create-event-obj
  [sm data]
  (let [new-uuid (uuid)]
    (reset! es-coll (assoc @es-coll new-uuid {:data data}))
    new-uuid))

(defmethod create-event-source "mock"
  [sm data unique-tag]
  (let [new-uuid (create-event-obj sm data)]
    (reset! es-coll (assoc-in @es-coll [new-uuid :unique-tag] unique-tag))
    new-uuid))

(defmethod get-event-source "mock"
  [sm uuid]
  (if-let [doc (get @es-coll uuid)]
    (assoc doc :uuid uuid)
    (failure 404 "Couldn't find event source.")))

(defmethod lookup-event-source "mock"
  [sm unique-tag]
  (mapv
   #(assoc (get %1 1) :uuid (get %1 0))
   (filterv
    #(= (:unique-tag (get %1 1)) unique-tag)
    (seq @es-coll))))

(defmethod update-event-source "mock"
  [sm uuid data]
  (reset! es-coll (assoc-in @es-coll [uuid :data] data))
  (assoc (get @es-coll uuid) :uuid uuid)
  uuid)

(defmethod delete-event-source "mock"
  [sm uuid]
  (reset! es-coll (dissoc @es-coll uuid))
  uuid)

(defmethod create-event "mock"
  [sm source-uuid data]
  (let [new-uuid (uuid)]
    (reset! e-coll (assoc @e-coll new-uuid {:data data}))
    new-uuid))

(defmethod get-event "mock"
  [sm uuid]
  (if-let [doc (get @e-coll uuid)]
    (assoc doc :uuid uuid)
    (failure 404 "Couldn't find event.")))

(defmethod update-event "mock"
  [sm uuid data]
  (reset! e-coll (assoc-in @e-coll [uuid :data] data))
  (assoc (get @e-coll uuid) :uuid uuid)
  uuid)

(defmethod delete-event "mock"
  [sm uuid]
  (reset! e-coll (dissoc @e-coll uuid))
  uuid)







