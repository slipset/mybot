(ns joint-bot.core
  (:require [xmpp-clj :as xmpp]
            [xmpp-clj.bot :as bot])
  (:import  [java.util.concurrent ExecutionException]
            [java.io StringWriter]
            [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
            [org.jivesoftware.smack.packet Message Presence Presence$Type]
            [org.jivesoftware.smackx.muc MultiUserChat]))



(defn packet-listener [conn processor]
  (reify PacketListener
    (processPacket [_ packet]
      (processor conn packet))))

(defn message->map [#^Message m]
  (try
    {:body (.getBody m)
     :from (.getFrom m)}
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
  (let [muc (MultiUserChat. conn (str room "@conf.hipchat.com"))]
    (.join muc room-nickname)
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

(defn message-handler [{:keys [body from] :as message} ]
  (def message message)
  (when (not= from "107552_tango@conf.hipchat.com/Joint Bot")
    (str "Why you tell me this: " body)))
                        

(.disconnect chat)
(.leave r)
(def chat (connect "107552_1112696" "Joint123" "Joint Bot"))
(def r (join chat  "107552_tango" "Joint Bot"  message-handler))

(.sendMessage r "cant really hear you")



