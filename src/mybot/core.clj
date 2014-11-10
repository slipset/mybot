(ns mybot.core
    (:require [mybot.xmpp :as xmpp]
              [mybot.util :as util]
              [clj-http.client :as client]))

(def xmpp {:host "localhost"
          :port 5222
          :username "sausagebot"
          :password "sausage"
          :resource "Mr. Sausage"
          :room "clojure@conference.clojutre" })

(defn issue [issue]
  (let [url (str "http://localhost:8080/rest/api/latest/issue/" issue)]
    (:body (client/get url
                       {:as :json
                        :basic-auth ["sausagebot" "sausage"]
                        :content-type :json
                        }))))

(defn format-issue [issue]
  (str "http://localhost:8080/browse/" (:key issue) " "
       (get-in issue [:fields :summary]) ", status: "
       (get-in issue [:fields :status :name]) ", assignee: "
       (get-in issue [:fields :assignee :name])))

(defn show-issue [issue-id]
  (-> issue-id
      (issue)
      (format-issue)))

(defn from-me [{:keys [from]}]
  (util/contains from (str (:room xmpp) "/" (:username xmpp))))

(defn to-me [{:keys [body]}]
  (util/contains body (:username xmpp)))

(defn tell-where []
  (let [{:keys [host port]} (util/runtime-info)]
    (str "Oh, I'm running on " host ":" port)))

(defn remove-handle [body]
  (util/remove-str body (str "@" (:username xmpp) " ")))

(defn handle-command [{:keys [body from] :as message}]
  (let [command (remove-handle body)]
    (cond (util/starts-with command "where are you running") (tell-where))))

(defn handle-noise [{:keys [body]}]
  (let [issue (util/matches body #".*(CLOJ-\d+).*")]
    (cond (util/contains body "clojure") "clojure rocks!"
          issue (show-issue issue))))
  
(defn handle-chatter [message]
  (if (to-me message)
    (handle-command message) 
    (handle-noise message)))

(def msgs (atom []))

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
(def chat (xmpp/connect xmpp))
(def clojure-room (xmpp/join chat (:room xmpp) (:username xmpp)))

(.sendMessage clojure-room "Hello! Clojutre!!")

(xmpp/add-message-listener (store-message (dont-reply-to-self #'handle-chatter)) clojure-room)
)
