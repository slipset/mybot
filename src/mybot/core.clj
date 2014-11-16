(ns mybot.core
    (:require [xmpp-clj.bot :as xmpp]
              [clj-http.client :as client]))

(def config {:host "localhost"
             :port 5222
             :username "sausagebot"
             :domain "localhost"
             :password "sausage"
             :nick "sausagebot"
             :resource "Mr. Sausage"
             :room "clojure@conference.clojutre"})

(defn get-issue [issue]
  (let [url (str "http://localhost:8080/rest/api/latest/issue/" issue)]
    (:body (client/get url {:as :json
                            :basic-auth [(:username config) (:password config)]
                            :content-type :json
                            }))))

(defn format-issue [issue]
  (str "http://localhost:8080/browse/" (:key issue) " "
       (get-in issue [:fields :summary]) ", status: "
       (get-in issue [:fields :status :name]) ", assignee: "
       (get-in issue [:fields :assignee :name])))

(defn show-issue [issue-id]
  (-> issue-id
      (get-issue)
      (format-issue)))

(defn runtime-info []
  {:host (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
   :port  (slurp "target/repl-port")})

(defn tell-where []
  (let [{:keys [host port]} (runtime-info)]
    (str "Oh, I'm running on " host ":" port)))

(defn remove-nick [body]
  (clojure.string/replace body (re-pattern (str "@" (:nick config) " ")) ""))

(defn handle-command [{:keys [body from] :as message}]
  (let [command (remove-nick body)]
    (cond (.startsWith command "where are you running") (tell-where)
          :else "I'm sorry, I don't know how to do that")))

(defn handle-noise [{:keys [body]}]
  (let [issue (second (re-matches #".*(CLOJ-\d+).*" body))]
    (cond (.contains body "clojure") "clojure rocks!"
          issue (show-issue issue))))

(defn to-me? [{:keys [body]}]
  (.contains body (:username config)))

(defn handle-chatter [message]
  (if (to-me? message)
    (handle-command message) 
    (handle-noise message)))

(defn from-me? [{:keys [from]}]
  (.contains from (str (:room config) "/" (:nick config))))

(defn remove-message [p handler]
  (fn [message]
    (when-not (p message)
      (handler message))))

(def msgs (atom []))

(defn store-message [handler]
  (fn [message]
    (swap! msgs conj message)
    (handler message)))

(def message-listener (->> handle-chatter
                          (store-message)
                          (remove-message from-me?)))
  
(comment 
  (reset! msgs [])
  (.disconnect chat)
  (def chat (xmpp/start config #(%)))
  (def clojure-room (xmpp-clj.bot/join chat (:room config) (:nick config)))

  (.sendMessage clojure-room "Hello! Clojutre!!")
  (.sendMessage clojure-room "clojure rocks")

  (xmpp/add-muc-listener clojure-room #'message-listener)
)
