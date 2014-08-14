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

(defn post-url [url body]
  (client/post url
               {
                :form-params
                :as :xml
                :accept :json
                :content-type :xml
                :basic-auth ["erik.assum" "Joint123"]
                })

(defn build-params [host]
  (str "name=host&value=" host))

(defn build! [id host]
  (let [url (str start-build-url "bt16&" (build-params host) "&modificationId=109463")]
    (get-url url)))


  
