(ns mybot.xmpp
    (:import  [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
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
       (handler muc message))))

(defn wrap-responder [handler out]
  (fn [muc message]
    (if-let [resp (handler message)]
      (.sendMessage out resp))))

(defn join
  [conn room room-nickname]
  (let [muc (MultiUserChat. conn room)
        history (DiscussionHistory.)]
    (.setMaxStanzas history 0)
    (.join muc room-nickname nil history, 3000)
    muc))


(defn add-message-listener [handler channel]
    (.addMessageListener channel
                         (packet-listener channel
                                          (with-message-map
                                            (wrap-responder handler channel)))))

(defn connect [{:keys [host port username password resource]}]
  (let [conn (XMPPConnection. (ConnectionConfiguration. host port))]
    (.connect conn)
    (try
      (.login conn username password resource)
      (catch XMPPException e
        (throw (Exception. "Couldn't log in with user's credentials."))))
    (.sendPacket conn (Presence. Presence$Type/available))
    conn))
    
