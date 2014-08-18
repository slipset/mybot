(ns joint-bot.teamcity
    (:require [clj-http.client :as client]))

(def api-url "http://git.joint.no:8111/app/rest/")

(defn builds-url [build branch]
  (let [b (if (= "master" branch)
            "&lt;default&gt;"
            branch)]
    (str api-url "builds?locator=status:success,buildType:" build ",branch:" b)))

(def start-build-url (str api-url  "buildQueue"))

(defn build-url [build-id]
  (str api-url "builds/id:" build-id))

(defn get-url [url]
  (let [response (client/get url
                              {
                               :debug true
                               :debug-body true
                               :as :json
                               :accept :json
                               :content-type :json
                               :basic-auth ["erik.assum" "Joint123"]
                               })] (:body response)))
(defn build-params [build-dep host]
  (str "name=host&value=" host "&name=dep.bt10.env.BUILD_NUMBER&value=" build-dep))

(defn post-url [url body]
  (let [response (client/post url
                              {:body body
                               :content-type :xml
                               :basic-auth ["erik.assum" "Joint123"]
                               })] (:body response)))

(defn build-params [build-type build-dep host who]
  (str "<build> 
    <triggeringOptions queueAtTop=\"true\"/>
  <buildType id=\"" build-type"\"/>
     <custom-artifact-dependencies count=\"1\">
        <artifact-dependency id=\"0\" type=\"artifact_dependency\">
            <properties>
                <property name=\"cleanDestinationDirectory\" value=\"true\"/>
                <property name=\"pathRules\" value=\"**\"/>
                <property name=\"revisionName\" value=\"buildNumber\"/>
                <property name=\"revisionValue\" value=\"13083\"/>
            </properties>
        <source-buildType id=\"bt10\" />
        </artifact-dependency>
    </custom-artifact-dependencies>
  <properties>
        <property name=\"host\" value=\"" host "\"/>
        <property name=\"owner\" value=\"" who "\"/>
    </properties>
</build>"))

(defn deploy! [who id host]
  (let [url start-build-url]
    (post-url url (build-params "bt16" id host))))

  
