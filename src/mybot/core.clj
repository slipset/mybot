(ns mybot.core
  (:require [mybot.xmpp :as xmpp]
            [mybot.jira :as jira]
            [postal.core :as postal]
            [mybot.config :as config]
            [mybot.teamcity :as tc]))

(def msgs (atom []))

(defn from-me [{:keys [from]}]
  (not= -1 (.indexOf from "Joint Bot")))

(defn to-me [{:keys [body]}]
  (= 0 (.indexOf body "@jointbot")))


(defn present-issue [issue]
  (let [i (jira/issue issue)]
    (str "http://jira.joint.no/browse/" issue ": " (get-in i [:fields :summary]) ", status: " (get-in i [:fields :status :name]))))

(def repl (atom []))

(defn starts-with [s p]
  (= 0 (.indexOf s p)))

(defn handle-noise [{:keys [body]}]
  (let [issue (re-matches config/getenv [:jira :issue-matcher] body)]
    (cond issue (present-issue (second issue)))))

(defn parse-date [s]
  (.format
    (java.text.SimpleDateFormat. "dd/MM HH:mm")
    (.parse
      (java.text.SimpleDateFormat. "yyyyMMdd'T'HHmmssZ")
      s)))

(defn latest-artifact-build [branch]
  (get-in (tc/latest-artifact branch) [:build 0]))

(defn latest-artifact-date [branch]
  (-> (latest-artifact-build branch)
      :id
       (tc/get-build)
       :finishDate
        parse-date))

(defn send-deploy-request [{:keys [to subject body] :as request}]
  (postal/send-message (config/getenv [:deploy :host])
                       {:from (config/getevn [:deploy :from])
                        :to to
                        :cc (config/getevn [:deploy :cc])
                        :subject subject
                        :body body})
  request)

(defn deploy-request [{:keys [to branch where]}]
  (let [url (tc/get-artifacts-url branch)]
    {:subject (str "New deploy of " branch " to " where)
     :body  (str "\n\nPlease deploy\n\n"
                  url
                  "\n\nto " where ".\n\n"
                  (config/getenv [:deploy :message]))
     :to to}))

(defn reply-to-deploy-request [{:keys [to branch where]}]
  (str "request to deploy " branch " to " where " is sent to " to))

(defn latest-build-id [branch]
  (-> (latest-artifact-build branch)
      :number))

(defn teamcity-rebuild [branch who] 
  (tc/rebuild! who branch)
  (str "Ok, rebuilding " branch " just for you :)"))

(defn parse-deploy [command]
  (let [terms (clojure.string/split command #" ")]
    {:branch (second terms)
     :host (nth terms 3)}))

(defn parse-deploy-request [command]
  (let [terms (clojure.string/split command #" ")]
		  {:to (nth terms 2)
		   :branch (nth terms 5)
		   :where (nth terms 7)}))

(defn branch->issue-info [branch]
  (when (not= "master" branch)
	  (-> branch
              (clojure.string/split #"\/")
              (second)
              (present-issue))))

(defn deploy-latest [{:keys [branch host who]}]
  (tc/deploy! who (latest-build-id branch) host)
  (str who " is deploying " branch " to " host "\n" (branch->issue-info branch)))

(defn runtime-info []
  {:host (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
   :port  (slurp "target/repl-port")})


(defn tell-where []
  (let [{:keys [host port]} (runtime-info)]
    (str "oh, I'm running on " host ":" port)))

(def commands
  [{:match #(starts-with % "latest artifact")
    :command (fn [{:keys [body]}]latest-artifact-date (last (clojure.string/split body #" ")))}
   {:match #(starts-with % "deploy")
    :command (fn [{:keys [body from]}]
               (deploy-latest (assoc (parse-deploy body) :who (second (clojure.string/split from #"/")))))}
   {:match #(starts-with % "rebuild")
    :command (fn [{:keys [body from]}]
               (teamcity-rebuild (last (clojure.string/split body #" "))
                                 (second (clojure.string/split from #"/"))))}
   {:match #(starts-with % "spank")
    :command (fn [{:keys [from]}]
               (str "Come on over here " (apply str (clojure.string/split from #" ")) " and I'll give you a real spanking!"))}
   {:match #(starts-with % "wakeup!")
    :command (fn [_] "Shut up! I'm already awake, stupid!")}
   {:match #(starts-with % "please ask")
    :command (fn [{:keys [body]}]
               (-> (parse-deploy-request body)
                   (deploy-request)
                   (send-deploy-request)
                   (reply-to-deploy-request)))}
   {:match #(starts-with % "where are you running")
    :command (fn [_] (tell-where))}])

   
   
(defn handle-command [{:keys [body from] :as message}]
  (let [command (clojure.string/replace body #"@jointbot " "")
        f (first (filter #((:match %) command) commands))]
        ((:command f) (assoc message :body command))))
3
(defn handle-noise [{:keys [body]}]
  (let [issues (re-seq ( config/getenv [:jira :issue-matcher]) body)]
    (cond issues (clojure.string/join "\n" (map #(present-issue (first %)) issues))
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

(defn build-failed [build]
  (let [branch (apply str (->> build
                               :snapshot-dependencies
                                :build
                                 (map :branchName)
                                 (filter #(not= "master" %))
                                 (interpose ", ")))]
                                 
                           
    (str "artifact build for " branch " failed: " (:webUrl build))))

(defmulti format-teamcity  build-type)

(defn get-who [build message]
  (or (:value (first (filter #(= "owner" (:name %)) (:property (:properties build)))))
      (second (re-matches #".*was triggered by <strong>(.*)</strong>" (:body message)))))
  
(defmethod format-teamcity :deploy [message build]
  (let [host  (get-in build [:properties :property 0 :value])
        branch (get-in build [:artifact-dependencies :build 0 :branchName])
        who (get-who build message)]
    (str "Yo " who ", your build " branch " was deployed to " host)))

(defmethod format-teamcity :artifacts [message build]
  (let [body (:body message)
        branch (get-in build [:artifact-dependencies :build 0 :branchName])]
    (if (re-matches #".*success.*" body)
      (str branch " is ready for deploy\n" (branch->issue-info branch))
      (build-failed build))))

(defn handle-teamcity [message]
  (swap! msgs conj message)
  (let [build-id (teamcity-build-id message)
        build (tc/get-build build-id)]
        (format-teamcity message build)))

(defn message-handler [message]
  (when-not (from-me message)
    (handle-chatter message)))

(defn auto-message-handler [message]
  (when-not (from-me message)
    (cond (not= -1 (.indexOf (:from message) "TeamCity"))
          (handle-teamcity message)
          :else nil)))
(defn join-room [chat name room]
  (assoc room :channel (xmpp/join chat  (:name room) name)))

(defn setup  [] 
  (.disconnect chat)
  (.leave tango-auto)
  (let [xmpp (config/getenv [:xmpp])
        chat (xmpp/connect xmpp)
        rooms (map (partial join-room chat name) (:rooms xmpp))]
    (map #(xmpp/add-message-listener message-handler (:channel %)) rooms)))
    



