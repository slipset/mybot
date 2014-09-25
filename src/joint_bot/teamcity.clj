(ns joint-bot.teamcity
    (:require [clj-http.client :as client]))

(def api-url "http://git.joint.no:8111/app/rest/")

(defn- builds-url [build branch]
  (let [b branch]
    (str api-url "builds?locator=status:success,buildType:" build ",branch:" b)))

(def start-build-url (str api-url  "buildQueue"))

(def host-mapping {
                   "deploy" "ph-deploy.joint.no"
                   "mast1" "ph-deploy-mast1.joint.no"
                   "mast2" "ph-deploy-mast2.joint.no"
                   "maste" "ph-deploy-maste.joint.no"
                   "test" "ph-test.joint.no"})

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
                               :basic-auth ["erik.assum" "Joint123"]
                               })] (:body response)))
(defn- post-url [url body]
  (let [response (client/post url
                              {:body body
                               :debug true
                               :debug-body true
                              
                               :content-type :xml
                               :basic-auth ["erik.assum" "Joint123"]
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
        artifact (first (filter #(re-matches #".*installation_package-public-.*.zip" (:name %)) (:files artifacts)))]
    (str "http://git.joint.no:8111/repository/download/bt10/" id ":id/" (:name artifact))))
