(ns mybot.teamcity
    (:require [clj-http.client :as client]
              [mybot.config :as config]))

(def api-url (config/getenv [:teamcity :url]))

(defn- builds-url [build branch]
  (let [b branch]
    (str api-url "builds?locator=status:success,buildType:" build ",branch:" b)))

(def start-build-url (str api-url  "buildQueue"))

(def host-mapping (config/getenv [:teamcity :host-mapping]))

(defn artifacts-url [id]
  (str api-url "builds/" id "/artifacts"))

(defn- build-url [build-id]
  (str api-url "builds/id:" build-id))

(defn- get-url [url]
  (let [response (client/get url
                              {
;;                               :debug true
;;                               :debug-body true
                               :as :json
                               :accept :json
                               :content-type :json
                               :basic-auth (config/getenv [:teamcity :basic-auth])
                               })] (:body response)))
(defn- post-url [url body]
  (let [response (client/post url
                              {:body body
                               :debug true
                               :debug-body true
                              
                               :content-type :xml
                               :basic-auth (config/getenv [:teamcity :basic-auth])
                               })] (:body response)))

(defn- correct-branch [branch]
  (if (= "master" branch)
    "&lt;default&gt;"
    branch))

(defn- build-params [build-type build-dep host who]
  (str "<build> 
    <triggeringOptions queueAtTop=\"true\"/>
  <buildType id=\"" build-type"\"/>
     <custom-artifact-dependencies count=\"1\">
        <artifact-dependency id=\"0\" type=\"artifact_dependency\">
            <properties>
                <property name=\"cleanDestinationDirectory\" value=\"true\"/>
                <property name=\"pathRules\" value=\"**\"/>
                <property name=\"revisionName\" value=\"buildNumber\"/>
                <property name=\"revisionValue\" value=\"" build-dep "\"/>
            </properties>
        <source-buildType id=\"bt10\" />
        </artifact-dependency>
    </custom-artifact-dependencies>
  <properties>
        <property name=\"host\" value=\"" host "\"/>
        <property name=\"owner\" value=\"" who "\"/>
    </properties>
</build>"))

(defn- build-rebuild-params [who branch]
  (str "<build branchName=\"" branch "\"> 
    <triggeringOptions queueAtTop=\"true\" rebuildAllDependencies=\"true\" cleanSources=\"true\"/>
  <buildType id=\"bt10\"/>
  <properties>
        <property name=\"owner\" value=\"" who "\"/>
    </properties>
</build>"))


(defn deploy! [who id host]
  (let [url start-build-url
        build-id (if (= "test" host)
                   "bt17"
                   "bt16")]
    (post-url url (build-params build-id id (get host-mapping host) who))))

(defn rebuild! [who branch]
  (let [url start-build-url]
    (post-url url (build-rebuild-params who (correct-branch branch)))))

(defn get-build [id]
  (get-url (build-url id)))
  
(defn latest-artifact [branch]
  (get-url (builds-url "bt10" (correct-branch branch))))

(defn get-artifacts-url [branch]
  (let [id (get-in (latest-artifact branch) [:build 0 :id])
        artifacts (get-url (artifacts-url id))
        artifact (first (filter #(re-matches (config/getenv [:teamcity :artifact-matcher]) (:name %)) (:files artifacts)))]
    (str (config/getenv [:teamcity :artifact-url]) id ":id/" (:name artifact))))
