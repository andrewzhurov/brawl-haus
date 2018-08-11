(ns brawl-haus.events-test
  (:require [brawl-haus.events :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            #?(:clj [matcho.core :refer [match]])))

#?(:clj
   (t/deftest events-test
     (t/testing "drive"
       (let [init-state {}
             conn-id "a-connection-id"
             state (-> init-state
                       (sut/drive [:conn/on-create conn-id])
                       )]

         (match {:location-id :race-panel}
                (sut/location state conn-id))
         ))))
