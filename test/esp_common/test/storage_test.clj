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

