(ns brawl-haus.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [[:body {:overflow-y "hidden"}]
   [:.tube-indicator {:position "absolute"
                      :height "15px" :width "15px"
                      :top "10px"
                      :right "10px"
                      :border-radius "50%"
                      :border-style "solid"
                      :border-width "3px"}
    [:&.true {:border-color "green"}]
    [:&.false {:border-color "gray"}]]
   [:.chat {:display "flex"
            :flex-direction "column"
            :height "100vh"
            :align-items "center"}
    [:.messages {:flex 1
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
    [:.send-box {:width "90%"
                 :display "flex"
                 :align-items "center"}
     [:input {:max-width "90%"
              :flex 1}]
     [:div.btn {:transition "0.2s"}]]]
   [:.collection.open-races {:display "flex"
                  :flex-direction "column"}
    [:.race {;:padding "5px 10px"
             ;:margin "3px 0px"
             :cursor "pointer"
             ;:background-color "lightgray"
             :display "flex"
             :flex-direction "row"
             :align-items "center"
             }]
    [:.countdown {:width "30px"}]
    ]
   [:.races-panel
    [:.new-race-btn {:margin "5px 5px"}]
    ]
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
     [:.nick {:display "inline-block"}]
     [:.average-speed {:display "inline-block"
                       :float "right"
                       :margin-right "10px"}]]]
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
