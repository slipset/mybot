(ns joint-bot.core
  (:require [xmpp-clj :as xmpp]
            [xmpp-clj.bot :as bot]
            [joint-bot.jira :as jira]
            [joint-bot.teamcity :as tc])
  (:import  [java.util.concurrent ExecutionException]
            [java.io StringWriter]
            [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
            [org.jivesoftware.smack.packet Message Presence Presence$Type]
            [org.jivesoftware.smackx.muc MultiUserChat DiscussionHistory]
            [org.jivesoftware.smack.util StringUtils]))



(defn packet-listener [conn processor]
  (reify PacketListener
    (processPacket [_ packet]
      (processor conn packet))))

(defn error->map [e]
  (if (nil? e) 
    nil
    {:code (.getCode e) :message (.getMessage e)}))

(defn message->map [#^Message m]
  (try
   {:body (.getBody m)
    :subject (.getSubject m)
    :thread (.getThread m)
    :from (.getFrom m)
    :from-name (StringUtils/parseBareAddress (.getFrom m))
    :to (.getTo m)
    :packet-id (.getPacketID m)
    :error (error->map (.getError m))
    :raw m
    :type (keyword (str (.getType m)))}
   (catch Exception e (println e) {})))

(defn with-message-map [handler]
  (fn [muc packet]
    (let [message (message->map #^Message packet)]
      (try
       (handler muc message)
       (catch Exception e (println e))))))

(defn wrap-responder [handler]
  (fn [muc message]
    (if-let [resp (handler message)]
      (.sendMessage muc resp))))

(defn join
  [conn room room-nickname handler]
  (let [muc (MultiUserChat. conn (str room "@conf.hipchat.com"))
        history (DiscussionHistory.)]
    (.setMaxStanzas history 0)
    (.join muc room-nickname nil history, 3000)
    (.addMessageListener muc
                         (packet-listener muc
                                          (with-message-map
                                                         (wrap-responder handler))))
    muc))


(defn connect
  [username password resource]
  (let [conn (XMPPConnection. (ConnectionConfiguration. "chat.hipchat.com" 5222))]
    (.connect conn)
    (try
      (.login conn username password resource)
      (catch XMPPException e
        (throw (Exception. "Couldn't log in with user's credentials."))))
    (.sendPacket conn (Presence. Presence$Type/available))
    conn))

(def msgs (atom []))

(defn from-me [{:keys [from]}]
  (not= -1 (.indexOf from "Joint Bot")))

(defn to-me [{:keys [body]}]
  (= 0 (.indexOf body "@jointbot")))


(defn present-issue [issue]
  (let [i (jira/issue issue)]
    (str "http://jira.joint.no/browse/" issue ": " (get-in i [:fields :summary]) ", status: " (get-in i [:fields :status :name]))))

(def repl (atom []))
(defn handle-command [{:keys [body]}]
  (let [msg (str "I don't know how to do that: " body)]
    (swap! repl conj msg)
    msg))

(defn handle-chatter [{:keys [to from body] :as message}]
  (swap! msgs conj message)
  (if (to-me message)
    (handle-command message)
    (if-let [issue (re-matches #".*((DARWIN|FRA)-\d+).*" body)]
      (present-issue (second issue)))))

(defn message-handler [message]
  (when-not (from-me message)
    (handle-chatter message)))
    

(comment
  (.disconnect chat)
  (.leave r))
(def chat (connect "107552_1112696" "Joint123" "Joint Bot"))
(def r (join chat  "107552_tango" "Joint Bot"  message-handler))
(def f (join chat  "107552_fram_-_development" "Joint Bot"  message-handler))






