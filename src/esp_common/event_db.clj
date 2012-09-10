(ns esp-common.event-db
  (:use [slingshot.slingshot :only [try+ throw+]]
        clojure-commons.error-codes
        esp-common.uuid
        esp-common.storage)
  (:require [clj-http.client :as cl]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [clojure-commons.file-utils :as ft]
            [cemerick.url :as url]))

(defn riak-obj-path
  "Constructs the path portion of a URL for a Riak request."
  [bucket key]
  (ft/path-join "/buckets" bucket "keys" key))

(defn fix-port
  "Turns port into an integer if it isn't already one."
  [port]
  (if (integer? port)
    port
    (Integer/parseInt port)))

(defn riak-url
  "Puts together a URL for a Riak HTTP API request."
  [base port bucket key & {:as url-query}]
  (-> (url/url base :path (riak-obj-path bucket key))
      (assoc :query url-query
             :port (fix-port port))
      (str)))

(defn url-from-record
  "Turns a storage-map and a uuid into a URL for a Riak HTTP API request.

   The storage map should have the following format:
   {:riak-base \"base Riak url\"
    :riak-port \"Riak port \"
    :riak-buck \"Riak bucket\"}"
  [storage-map uuid]
  (riak-url
   (:riak-base storage-map)
   (:riak-port storage-map)
   (:riak-bucket storage-map)
   uuid))

(defn request-failed
  "Throws an exception for a failed request."
  [resp]
  (throw+ {:error_code ERR_REQUEST_FAILED
           :status     (:status resp)
           :body       (:body resp)}))

(defn get-object
  "Issues a GET request for an object stored in Riak."
  [storage-map uuid]
  (let [obj-url (url-from-record storage-map uuid)
        resp (cl/get obj-url {:throw-exceptions false})]
    (cond
     (<= 200 (:status resp) 299)  (json/read-json (:body resp))
     (= (:status resp) 404)       (hash-map)
     :else                        (request-failed resp))))

(defn put-object
  "Issues a PUT request to place an object into Riak."
  [storage-map uuid obj-map & [extra-headers]]
  (let [obj-url (url-from-record storage-map uuid)
        resp (cl/put
              obj-url
              {:content-type :json
               :body (json/json-str obj-map)
               :headers (merge
                         extra-headers
                         {"X-Riak-Meta-ESP-LastModified" (str (time/now))})}
              {:throw-exceptions false})]
    (cond 
     (<= 200 (:status resp) 299) uuid
     :else                       (request-failed resp))))

(defn post-object
  "Issues a POST request to update an object in Riak."
  [storage-map uuid obj-map & [extra-headers]]
  (let [obj-url (url-from-record storage-map uuid)
        resp (cl/post
              obj-url
              {:content-type :json
               :body (json/json-str obj-map)
               :headers (merge
                         extra-headers
                         {"X-Riak-Meta-ESP-LastModified" (str (time/now))})}
              {:throw-exceptions false})]
    (cond
     (<= 200 (:status resp) 299) uuid
     :else                       (request-failed resp))))

(defn delete-object
  "Issues a DELETE request to delete an object from Riak."
  [storage-map uuid]
  (let [obj-url (url-from-record storage-map uuid)
        resp (cl/delete obj-url {:throw-exceptions false})]
    (cond
      (<= 200 (:status resp) 299) uuid
      :else                       (request-failed resp))))

(defmethod get-event-source "riak"
  [sm uuid]
  (get-object sm uuid))

(defmethod create-event-source "riak"
  [sm data unique-tag]
  (put-object sm (uuid) data {"X-Riak-Meta-ESP-UniqueTag" unique-tag}))

(defmethod update-event-source "riak"
  [sm uuid data]
  (post-object sm uuid data))

(defmethod delete-event-source "riak"
  [sm uuid]
  (delete-object sm uuid))

(defmethod create-event "riak"
  [sm src-uuid data]
  (put-object sm (uuid) data {"X-Riak-Meta-ESP-EventSource" src-uuid}))

(defmethod get-event "riak"
  [sm uuid]
  (get-object sm uuid))

(defmethod update-event "riak"
  [sm uuid data]
  (post-object sm uuid data))

(defmethod delete-event "riak"
  [sm uuid]
  (delete-object sm uuid))






