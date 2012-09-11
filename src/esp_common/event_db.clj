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

(defn handle-get-response
  [resp]
  (cond
   (<= 200 (:status resp) 299)  (json/read-json (:body resp))
   (= (:status resp) 404)       (hash-map)
   :else                        (request-failed resp)))

(defn handle-put-post-response
  [resp]
  (cond 
   (<= 200 (:status resp) 299) uuid
   :else                       (request-failed resp)))

(defn handle-delete-response
  [resp]
  (cond
   (<= 200 (:status resp) 299) uuid
   :else                       (request-failed resp)))

(defn request-map
  [obj-map extra-headers]
  {:content-type :json
   :body (json/json-str obj-map)
   :headers (merge
             extra-headers
             {"X-Riak-Meta-ESP-LastModified" (str (time/now))})})

(defn get-object
  "Issues a GET request for an object stored in Riak."
  [storage-map uuid]
  (-> (url-from-record storage-map uuid)
      (cl/get {:throw-exceptions false})
      handle-get-response))

(defn put-object
  "Issues a PUT request to place an object into Riak."
  [storage-map uuid obj-map & [extra-headers]]
  (-> (url-from-record storage-map uuid)
      (cl/put (request-map obj-map extra-headers) {:throw-exceptions false})
      handle-put-post-response))

(defn post-object
  "Issues a POST request to update an object in Riak."
  [storage-map uuid obj-map & [extra-headers]]
  (-> (url-from-record storage-map uuid)
      (cl/post (request-map obj-map extra-headers) {:throw-exceptions false})
      handle-put-post-response))

(defn delete-object
  "Issues a DELETE request to delete an object from Riak."
  [storage-map uuid]
  (-> (url-from-record storage-map uuid)
      (cl/delete {:throw-exceptions false})
      handle-delete-response))

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






