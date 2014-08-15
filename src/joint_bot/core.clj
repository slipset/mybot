(ns joint-bot.core
  (:require [joint-bot.xmpp :as xmpp]
            [joint-bot.jira :as jira]
            [joint-bot.teamcity :as tc]))




(def msgs (atom []))

(defn from-me [{:keys [from]}]
  (not= -1 (.indexOf from "Joint Bot")))

(defn to-me [{:keys [body]}]
  (= 0 (.indexOf body "@jointbot")))


(defn present-issue [issue]
  (let [i (jira/issue issue)]
    (str "http://jira.joint.no/browse/" issue ": " (get-in i [:fields :summary]) ", status: " (get-in i [:fields :status :name]))))

(def repl (atom []))


(defn parse-date [s]
  (.format
    (java.text.SimpleDateFormat. "dd/MM HH:mm")
    (.parse
      (java.text.SimpleDateFormat. "yyyyMMdd'T'HHmmssZ")
      s)))
      
(defn latest-artifact [branch]
  (-> (tc/get-url (tc/builds-url "bt10" branch))
			:build
			first
			:id
			(tc/build-url)
			(tc/get-url)
                        :finishDate
                         parse-date))

(defn starts-with [s p]
  (= 0 (.indexOf s p)))

(def nasty-words #{"BITCH" "FUCK" "SHIT" "CUNT"})

(defn profanity? [body]
  (seq (filter identity (map #(re-matches (re-pattern (str ".* ?" % " ?.*")) (clojure.string/upper-case body)) nasty-words))))

(defn handle-command [{:keys [body]}]
  (let [command (clojure.string/replace body #"@jointbot " "")]
    (cond (starts-with command "latest artifact") (latest-artifact (last (clojure.string/split command #" ")))
          (starts-with command "spank") (str "Come on over here " (clojure.string/split command #" ") " and I'll give you a real spanking!")
          :else (str "I don't know how to " command))))


(defn handle-noise [{:keys [body]}]
  (let [issue (re-matches #".*((DARWIN|FRA)-\d+).*" body)]
    (cond issue (present-issue (second issue))
          (profanity? body) "Please, watch your language")))
  
(defn handle-chatter [{:keys [to from body] :as message}]
  (swap! msgs conj message)
  (if (to-me message)
    (handle-command message)
    (handle-noise message)))

(defn teamcity-build-id [{:keys [body]}]
  (second (re-matches #".*;buildId=(\d+)\".*" body)))


(defn format-teamcity [message build]
  (let [host  (get-in build [:properties :property 0 :value])
        branch (get-in build [:artifact-dependencies :build 0 :branchName])]
    (str branch " was deployed to " host)))

(defn build-type [message build]
  (let [body (:body message)]
    (cond (re-matches #".*projectId=ContinousIntegration.*" body) :artifacts
          (re-matches #".*projectId=Deployment.*" body) :deploy
          :else :unknown)))

(defmulti format-teamcity  build-type)

(defmethod format-teamcity :deploy [message build]
  (let [host  (get-in build [:properties :property 0 :value])
        branch (get-in build [:artifact-dependencies :build 0 :branchName])]
    (str branch " was deployed to " host)))

(defmethod format-teamcity :artifacts [message build]
  (let [body (:body message)
        branch (get-in build [:artifact-dependencies :build 0 :branchName])]
    (if (re-matches #".*success.*" body)
      (str branch " is ready for deploy" )
      (str "artifact build failed: " (:webUrl build)))))

(defn handle-teamcity [message]
  (swap! msgs conj message)
  (let [build-id (teamcity-build-id message)
        build (tc/get-url (tc/build-url build-id))]
        (format-teamcity message build)))

(defn message-handler [message]
  (when-not (from-me message)
    (handle-chatter message)))

(defn auto-message-handler [message]
  (when-not (from-me message)
    (cond (not= -1 (.indexOf (:from message) "TeamCity"))
          (handle-teamcity message)
          :else nil)))

(comment
  (.disconnect chat)
  (.leave tango-auto)


(def chat (xmpp/connect "107552_1112696" "Joint123" "Joint Bot"))

(def tango (xmpp/join chat  "107552_tango" "Joint Bot"))

(xmpp/add-message-listener message-handler tango tango)

(def tango-auto (xmpp/join chat  "107552_tango_env." "Joint Bot"))

(xmpp/add-message-listener auto-message-handler tango-auto tango)
  
(def fram (xmpp/join chat  "107552_fram_-_development" "Joint Bot"))
(xmpp/add-message-listener message-handler fram fram)

tango
tango-auto


(.sendMessage tango-auto "ugle")
)
