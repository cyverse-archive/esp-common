(ns esp-common.uuid
  (:require [clojure.string :as string]))

(defn uuid [] (string/upper-case (str (java.util.UUID/randomUUID))))