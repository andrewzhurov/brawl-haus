(ns brawl-haus.css
  (:require [garden.def :refer [defstyles]]))

(defn grid [& strs]
  (let [rows (butlast strs)
        columns (last strs)
        escaped-rows (for [row rows]
                       (let [[areas size] (clojure.string/split row #" (?=[\w\d]+$)")]
                         (format "\"%s\" %s" areas size)))]
    (str (clojure.string/join "\n"  (conj (vec escaped-rows)  (str "/ " columns))))))

(defstyles screen
  [[:.badge {:border-radius "4px"}]
   [:span.badge.new {:font-size "1.1rem"
                     :font-weight "500"}]
   [:body {:overflow-y "hidden"}]
   [:.tube-indicator {:position "absolute"
                      :height "15px" :width "15px"
                      :top "10px"
                      :right "10px"
                      :border-radius "50%"
                      :border-style "solid"
                      :border-width "3px"}
    [:&.true {:border-color "green"}]
    [:&.false {:border-color "gray"}]]
   [:.chat {:display "grid"
            :grid-template
            (grid "send-box participants 50px"
                  "messages participants 1fr"
                  "2fr 1fr")
            :height "100vh"
            }
    [:.messages {:grid-area "messages"
                 :min-height "0px"
                 :overflow-y "auto"
                 :width "100%"
                 :padding-left "5%"
                 :padding-right "5%"}
     [:ul {:display "flex"
           :flex-direction "column"}
      [:li {:margin-bottom "3px"
            :padding "10px 10px"
            :width "fit-content"
            :background-color "lightgray"
            :border-radius "8px"
            :margin-left "10px"
            :max-width "80%"}
       [:&.my {:background-color "lightblue"
               :align-self "flex-end"
               :margin-left "0px"
               :margin-right "10px"}]
       [:.from {:display "inline-block"
                :margin-right "10px"}]
       [:.received-at {:display "inline-block"}]]]
     [:.message {
                 }]]
    [:.send-box {:grid-area "send-box"
                 :padding-left "5%"
                 :padding-right "5%"
                 :display "flex"
                 :align-items "center"}
     [:input {:max-width "90%"
              :flex 1}]
     [:div.btn {:transition "0.2s"}]]
    [:ul.participants {:grid-area "participants"
                       :overflow-y "auto"
                       :margin "0px"
                       :height "100%"}
     [:li]
     [:a {:cursor "pointer"}]
     [:.collection-header {:position "sticky"
                           :top "0px"}]
     [:.activity-indicator
      ]]]
   [:.collection.open-races
    {:display "flex"
     :grid-area "open-races"
     :flex-direction "column"}
    [:.race {;:padding "5px 10px"
             ;:margin "3px 0px"
             :cursor "pointer"
             ;:background-color "lightgray"
             :display "flex"
             :flex-direction "row"
             :align-items "center"
             }]
    [:.countdown {:width "40px"}]
    ]
   [:.races-panel
    {:display "grid"
     :grid-template (grid "controls highscores 50px"
                          "open-races highscores 1fr"
                          "2fr 1fr")}
    [:.new-race-btn {:margin "5px 5px"}]
    [:ul.highscores {:grid-area "highscores"
                     :overflow-y "auto"
                     :margin "0px"
                     :height "100%"}
     [:li]
     [:a {:cursor "pointer"}]
     [:.collection-header {:position "sticky"
                           :top "0px"}]
     [:.activity-indicator
      ]]
    #_[:.highscores {:grid-area "highscores"
                   :overflow-y "auto"
                   :margin "0px"
                   :height "100%"}]]
   [:.race-panel {:padding "5px 10px"}
    [:.countdown {:text-align "center"
                  :font-size "20px"
                  :text-decoration "underline"
                  :margin "5px"}]
    [:.waiting {:vertical-align "center"}]
    [:.text-race 
     [:.race-text {:display "flex"
                   :flex-direction "row"
                   :flex-wrap "wrap"}
      [:.char {:display "block"
               :min-width "unset"
               :min-height "unset"}
       [:&.whitespace {:min-width "5px"}]
       [:&.done {
                 :color "gray"}]
       [:&.right-typed {:border-bottom "2px solid blue"}]
       [:&.wrong-typed {:border-bottom "2px solid red"}]]]
     [:input {:width "90%"
              :margin "auto"
              :display "block"}]]
    [:.race-progress {:margin "5px 0px"}
     [:.participant {:position "relative"}
      [:.nick {:display "inline-block"}]
      [:.average-speed {:display "inline-block"
                        :position "absolute"
                        :top 0 :right 0
                        :margin-right "10px"}]]]]
   [:.app {:display "grid"
           :grid-template-columns "auto 1fr"}
    [:.navbar {:display "flex"
                 :flex-direction "column"
                 :height "100vh"
                 :width "60px"
                 :border-right "1px solid lightgray"
                 :align-items "center"}
     [:.tab {:padding "0px 10px"
             :margin "3px 0px"
             :cursor "pointer"}
      [:i {:font-size "30px"}]
      ]]
    [:.content {:margin-left "10px"
                :box-shadow "0 2px 2px 0 rgba(0,0,0,0.14), 0 3px 1px -2px rgba(0,0,0,0.12), 0 1px 5px 0 rgba(0,0,0,0.2)"}]]
   [:.content {:margin-left "10px"}]

   [:.notifications {:position "absolute"
                     :top "10px"
                     :right "10px"}]

   [:.login-panel {:display "flex"
                   :flex-direction "column"
                   :align-items "center"
                   :justify-content "center"
                   :height "100vh"
                   :width "100vw"}
    [:form {:max-width "300px"
            :max-height "700px"}]
    [:.onward {:width "100%"
               :height "42px"}]]])
