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

(def out *out*)

(def chat (xmpp/connect config))

(def clojure-room (xmpp/join chat
                                 (:room config)
                                 (:nick config)))

(.sendMessage clojure-room "Hello! ClojuTre")
(.sendMessage clojure-room "Clojure rocks!")
















(defn handle-chatter [m])

(def message-listener handle-chatter)

(def message-sender (xmpp/dev-null))

(xmpp/add-listener clojure-room (xmpp/default-processor
                                  #'message-listener
                                    #'message-sender
                                      (xmpp/wrap-errors out)))

























(def message-listener (-> #'handle-chatter
                          (xmpp/wrap-tee (fn [m] (.println out m)))))



































(def msgs (atom []))

(defn store-message [message] (swap! msgs conj message))

(def message-listener (-> handle-chatter 
                          (xmpp/wrap-tee store-message)))






























(defn from-me? [{:keys [from]}]
  (.contains from
             (str (:room config) "/" (:nick config))))

(def message-listener (-> #'handle-chatter 
                          (xmpp/wrap-tee store-message)
                          (xmpp/wrap-remove-message from-me?)))





























(def message-sender (xmpp/create-sender :response))

(defn handle-noise [{:keys [body]}]
  (when (.contains body "clojure")
    "clojure rocks!"))

(defn handle-chatter [m]
  (handle-noise m))



























(defn get-issue [issue]
  (let [url (str "http://localhost:8080/rest/api/latest/issue/"
                 issue)]
    (:body (client/get url {:as :json
                            :basic-auth [(:username config)
                                         (:password config)]
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

(defn handle-noise [{:keys [body]}]
  (let [issue (second (re-matches #".*(CLOJ-\d+).*" body))]
    (cond issue (show-issue issue)
          (.contains body "clojure") "clojure rocks!")))













(defn handle-command [_]
  "I'm sorry, I don't know how to do that")

(defn to-me? [{:keys [body]}]
  (.startsWith  body (str "@" (:nick config))))

(defn handle-chatter [message]
  (if (to-me? message)
    (handle-command message) 
    (handle-noise message)))

























(defn runtime-info []
  {:host (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
   :port  (slurp "target/repl-port")})

(defn tell-where [{:keys [host port]}]
    (str "Oh, I'm running on " host ":" port))

(defn remove-nick [body]
  (clojure.string/replace body
           (re-pattern (str "^@" (:nick config) " ")) ""))

(defn handle-command [{:keys [body from] :as message}]
  (let [command (remove-nick body)]
    (cond (.startsWith command "where are you running")
          (tell-where (runtime-info))
          :else
           "I'm sorry, I don't know how to do that")))
