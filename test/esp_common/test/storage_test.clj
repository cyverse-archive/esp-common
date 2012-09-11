(ns esp-common.test.storage-test
  (:use esp-common.storage
        esp-common.event-db
        esp-common.mock-event-db
        [slingshot.slingshot :only [try+]]
        :reload)
  (:use midje.sweet))

(def sm {:storage-type "mock"})

(defn matches?
  [in-str]
  (pos?
   (count
    (re-seq
     #"[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}" in-str))))


(def euuid (create-event-obj sm {:foo "bar"}))

(fact
 euuid => matches?
 (update-event-source sm euuid {:baz "blippy"}) => matches?
 (update-event-source sm euuid {:more "data"}) => euuid
 (delete-event-source sm euuid) => euuid

 (try+
  (get-event-source sm euuid)
  (catch :error_code {:keys [error_code] :as error-map}
    error-map)) => {:error_code "ERR_REQUEST_FAILED"
                    :status 404
                    :body "Couldn't find event source."})

(def luuid (create-event-source sm {:foo "bar"} "testing"))

(fact
 luuid => matches?
 (lookup-event-source sm "testing") => [{:uuid luuid
                                         :unique-tag "testing"
                                         :data {:foo "bar"}}])

(def test-uuid (create-event sm luuid {:foo "bar"}))

(fact
 test-uuid => matches?
 (get-event sm test-uuid) => {:uuid test-uuid
                              :data {:foo "bar"}}
 (update-event sm test-uuid {:baz "blippy"}) => test-uuid
 (delete-event sm test-uuid) => test-uuid

 (try+
  (get-event sm test-uuid)
  (catch :error_code {:keys [error_code] :as error-map}
    error-map)) => {:error_code "ERR_REQUEST_FAILED"
                    :status 404
                    :body "Couldn't find event."})

;;;(esp-common.event-db) testing

(def riak-sm {:riak-base "http://testing.com"
              :riak-port "3333"
              :riak-bucket "bucket"})

(fact
 (riak-obj-path "foo" "bar") => "/buckets/foo/keys/bar"
 (fix-port 404) => 404
 (fix-port "404") => 404

 (riak-url "http://foo.com" 3333 "bucket" "key") =>
 "http://foo.com:3333/buckets/bucket/keys/key"

 (riak-url "http://foo.com" 3333 "bucket" "key" {:query "value"}) =>
 "http://foo.com:3333/buckets/bucket/keys/key?query=value"

 (url-from-record riak-sm "uuid") =>
 "http://testing.com:3333/buckets/bucket/keys/uuid"

 (handle-get-response {:status 200 :body "{\"test\":\"value\"}"}) =>
 {:test "value"}

 (handle-get-response {:status 204 :body "{\"test\":\"value\"}"}) =>
 {:test "value"}

 (handle-get-response {:status 299 :body "{\"test\":\"value\"}"}) =>
 {:test "value"}

 (handle-get-response {:status 404}) => {}

 (try+
  (handle-get-response {:status 500 :body "Hurr"})
  (catch :error_code error-map error-map)) =>
  {:error_code "ERR_REQUEST_FAILED"
   :status 500
   :body "Hurr"}

  (handle-put-post-response {:status 200} "uuid") => "uuid"
  (handle-put-post-response {:status 204} "uuid") => "uuid"
  (handle-put-post-response {:status 299} "uuid") => "uuid"

  (try+
   (handle-put-post-response {:status 500 :body "Derp"} "uuid")
   (catch :error_code error-map error-map)) =>
   {:error_code "ERR_REQUEST_FAILED"
    :status 500
    :body "Derp"}
   
   (handle-delete-response {:status 200} "uuid") => "uuid"
   (handle-delete-response {:status 204} "uuid") => "uuid"
   (handle-delete-response {:status 299} "uuid") => "uuid"
   
   (try+
    (handle-delete-response {:status 500 :body "Herp"} "uuid")
    (catch :error_code error-map error-map)) =>
    {:error_code "ERR_REQUEST_FAILED"
     :status 500
     :body "Herp"})

(fact
 (request-map {:foo "bar"} {"extra" "headers"}) => #(contains? %1 :content-type)
 (request-map {:foo "bar"} {"extra" "headers"}) => #(contains? %1 :body)
 (request-map {:foo "bar"} {"extra" "headers"}) => #(contains? %1 :headers)

 (get (request-map {:foo "bar"} {"extra" "headers"}) :content-type) => :json
 (get (request-map {:foo "bar"} {"extra" "headers"}) :body) =>
 "{\"foo\":\"bar\"}"

 (get-in
  (request-map {:foo "bar"} {"extra" "headers"})
  [:headers "X-Riak-Meta-ESP-LastModified"]) => string?

  (get-in
   (request-map {:foo "bar"} {"extra" "headers"})
   [:headers "extra"]) => "headers")

(fact
 (nest-data {} {:foo "bar"}) => {:foo "bar" :data {}})

