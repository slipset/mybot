(ns mybot.core
    (:require [mybot.xmpp :as xmpp]
              [mybot.util :as util]
              [mybot.jira :as jira]              
              [mybot.config :as config]))

(def msgs (atom []))

(defn from-me [{:keys [from]}]
  (util/contains from (str (config/getenv [:xmpp :room]) "/" (config/getenv [:xmpp :username]))))

(defn to-me [{:keys [body]}]
  (util/contains body (config/getenv [:xmpp :name])))

(defn tell-where []
  (let [{:keys [host port]} (util/runtime-info)]
    (str "Oh, I'm running on " host ":" port)))

(defn remove-handle [body]
  (util/remove-str body (str (config/getenv [:xmpp :name]) " ")))

(defn format-issue [issue]
  (str (jira/get-link issue) " "
       (get-in issue [:fields :summary]) ", status: "
       (get-in issue [:fields :status :name]) ", assignee: "
       (get-in issue [:fields :assignee :name])))

(defn show-issue [issue-id]
  (-> issue-id
      (jira/issue)
      (format-issue)))

(defn handle-command [{:keys [body from] :as message}]
  (let [command (remove-handle body)]
    (cond (util/starts-with body "where are you running") (tell-where))))

(defn handle-noise [{:keys [body]}]
  (let [issue (util/matches body (config/getenv [:jira :issue-matcher]))]
    (cond (util/contains body "clojure") "clojure rocks!"
          issue (show-issue issue))))
  
(defn handle-chatter [message]
  (if (to-me message)
    (handle-command message) 
    (handle-noise message)))

(defn store-message [handler]
  (fn [message]
    (swap! msgs conj message)
    (handler message)))

(defn dont-reply-to-self [handler]
  (fn [message]
    (when-not (from-me message)
      (handler message))))
    
(comment 
(reset! msgs [])
(def chat (xmpp/connect (config/getenv [:xmpp])))
(def clojure-room (xmpp/join chat
                             (config/getenv [:xmpp :room])
                             (config/getenv [:xmpp :username])))

(.sendMessage clojure-room "hello!")

(xmpp/add-message-listener (store-message (dont-reply-to-self #'handle-chatter)) clojure-room)
)
