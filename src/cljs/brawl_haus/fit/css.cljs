(ns brawl-haus.fit.css)

(def styles
  [[:body {:overflow "hidden"}]
   [:svg {:cursor "crosshair"
          }]
   [:.data-reactroot {:overflow "hidden"}]

   [:.timeline {:position "absolute"
                :top 0 :right 0 :left 0
                :height "40px"
                :background "rgba(0,0,0,0.3)"
                }
    [:input {:width "100%"}]
    [:.left {:position "absolute" :top "20px" :left "10px"}]
    [:.right {:position "absolute" :top "20px" :right "10px"}]]])
