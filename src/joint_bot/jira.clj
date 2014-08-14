(ns joint-bot.jira
    (:require [clj-http.client :as client]))



(def api-url "http://jira.joint.no/rest/api/latest/")

(def session (clj-http.cookies/cookie-store))

(defn issue-url [issue]
  (str api-url "issue/" issue))
  
(defn get-url [username password url]
  (let [response (client/get url
                              {
                               :debug true
                               :as :json
                               :cookie-store session
                               :basic-auth [username password]
                               :content-type :json
                               })] (:body response)))

(defn issue [issue]
  (get-url "ea@joint.no" "Joint123" (issue-url issue)))


