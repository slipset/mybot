(ns mybot.xmpp
    (:import  [java.util.concurrent ExecutionException]
              [java.io StringWriter]
              [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
              [org.jivesoftware.smack.packet Message Presence Presence$Type]
              [org.jivesoftware.smackx.muc MultiUserChat DiscussionHistory]
              [org.jivesoftware.smack.util StringUtils]
              [org.jivesoftware.smackx.packet VCard]))




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

(defn wrap-responder [handler out]
  (fn [muc message]
    (if-let [resp (handler message)]
      (.sendMessage out resp))))



(defn join
  [conn room room-nickname]
  (let [muc (MultiUserChat. conn (str room "@conf.hipchat.com"))
        history (DiscussionHistory.)]
    (.setMaxStanzas history 0)
    (.join muc room-nickname nil history, 3000)
    muc))


(defn add-message-listener [handler in out]
    (.addMessageListener in
                         (packet-listener in
                                          (with-message-map
                                            (wrap-responder handler out)))))

(defn connect [{:keys [host port username password resource]}]
  (let [conn (XMPPConnection. (ConnectionConfiguration. host port))]
    (.connect conn)
    (try
      (.login conn username password resource)
      (catch XMPPException e
        (throw (Exception. "Couldn't log in with user's credentials."))))
    (.sendPacket conn (Presence. Presence$Type/available))
    conn))
    
