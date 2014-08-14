(ns joint-bot.jira
    (:require [clj-http.client :as client]))



(def api-url "http://jira.joint.no/rest/api/latest/")

(defn issue-url [issue]
  (str api-url "issue/" issue))
  
(defn get-url [url]
  (let [response (client/get url
                              {
                               :debug true
                               :as :json
                               :basic-auth ["ea@joint.no" "Joint123"]
                               :content-type :json
                               })] (:body response)))

(defn issue [issue]
  (get-url (issue-url issue)))


