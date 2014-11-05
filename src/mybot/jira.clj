(ns mybot.jira
    (:require [clj-http.client :as client]
              [mybot.config :as config]))

(defn issue-url [issue]
  (str (config/getenv [:jira :api]) "issue/" issue))
  
(defn get-url [url]
  (let [response (client/get url
                             {:as :json
                              :basic-auth (config/getenv [:jira :basic-auth])
                              :content-type :json
                              })] (:body response)))

(defn issue [issue]
  (get-url (issue-url issue)))

(defn get-link [issue]
  (let [key (:key issue)]
    (str (config/getenv [:jira :url]) key)))

(comment
  (get-in (issue "CLOJ-1") [:fields :status :name])
  (get-in (issue "CLOJ-1") [:fields :assignee])
  (get-in (issue "CLOJ-1") [:fields :summary])
)

  
  
