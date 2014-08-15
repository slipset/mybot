(ns joint-bot.teamcity
    (:require [clj-http.client :as client]))

(def api-url "http://git.joint.no:8111/app/rest/")
(def start-build-url "http://erik.assum:Joint123@git.joint.no:8111/httpAuth/action.html?add2Queue=")
(defn builds-url [build branch]
  (str api-url "builds?locator=status:success,buildType:" build ",branch:" branch))

(defn build-url [build-id]
  (str api-url "builds/id:" build-id))

(defn get-url [url]
  (let [response (client/get url
                              {
                               :as :json
                               :accept :json
                               :content-type :json
                               :basic-auth ["erik.assum" "Joint123"]
                               })] (:body response)))

(defn build-params [build-dep host]
  (str "name=host&value=" host "&name=dep.bt10.env.BUILD_NUMBER&value=" build-dep))

(defn deploy! [id host]
  (let [url (str start-build-url "bt16&" (build-params id host) )]
    (get-url url)))


  
