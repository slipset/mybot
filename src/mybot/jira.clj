(ns mybot.jira
    (:require [clj-http.client :as client]
              [mybot.config :as config]))

(defn issue-url [issue]
  (str (config/getenv [:jira :url]) "issue/" issue))
  
(defn get-url [url]
  (let [response (client/get url
                              {:as :json
                               :basic-auth (config/getenv [:jira :basic-auth])
                               :content-type :json
                               })] (:body response)))

(defn issue [issue]
  (get-url (issue-url issue)))

