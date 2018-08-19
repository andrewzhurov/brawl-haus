(ns brawl-haus.panels.hiccup-touch
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub defview view]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]
            [brawl-haus.components :as comps]
            [paren-soup.core :as ps]
            [hiccups.runtime :as hiccups]
            [garden.core :as garden]
            #_[brawl-haus.events :as events]))

(defn my-eval [form cb]
  (cljs.js/eval (cljs.js/empty-state) form {:eval cljs.js/js-eval} #(cb (:value %))))

(defn get-h []
  (.-textContent (js/document.getElementById "hiccup-content")))
(defn get-g []
  (.-textContent (js/document.getElementById "g-content")))

(defn drive-on [evt]
  (fn []
    (rf/dispatch [:conn/send evt])))

(def hiccup-init-content
  "[:div#navigation-pannel
  [:div.item \"HOME\"]
  [:div.item \"TAB2\"]
  [:div.item \"TAB3\"]]")
(defn hiccup-editor []
  [:div.hiccup-editor.app
   [:div.content
    [:div.paren-soup
     [:link
      {:href "paren-soup/paren-soup-dark.css",
       :type "text/css",
       :rel "stylesheet"}]
     #_[:div.instarepl]
     [:div.numbers]
     [:div#hiccup-content.content
      {:contentEditable "true"}
      hiccup-init-content]]]])

(defn garden-editor []
  (r/create-class
   {:component-did-mount
    (fn [] (ps/init-all))
    :reagent-render
    (fn []
      [:div.garden-editor.app
       [:div.content
        [:div.paren-soup
         [:link
          {:href "paren-soup/paren-soup-dark.css",
           :type "text/css",
           :rel "stylesheet"}]
         #_[:div.instarepl]
         [:div.numbers]
         [:div#g-content.content
          {:contentEditable "true"}
          "[:#navigation-pannel {:display \"flex\"
                      :flex-direction \"column\"
                      :width \"100px\"
                      :border-right \"2px solid gray\"
                      :height \"100%\"}
 [:.item {:padding \"20px\"
          :text-align \"center\"
          :transition \"1s\"}
  [:&:hover {:background-color \"yellow\"}]
  ]]"]]]])}))

(defn render-result [h g]
  [:div#hr.render-result
   [:style (garden/css [:#hr g])]
   h])

(defn compile-result [h g]
  [:div.compile-result
   [:div.h-compiled (pr-str (hiccups/render-html h))]
   [:div.g-compiled (pr-str (garden/css [:#hr g]))]])

(defmethod panels/panel :hiccup-touch
  [{:keys [params]}]
  (let [h (rf/subscribe [:db/get-in [::h]])
        g (rf/subscribe [:db/get-in [::g]])
        h-looper (js/setInterval #(do (println "RERUN H")
                                      (my-eval (cljs.reader/read-string (get-h))
                                               (fn [h] (rf/dispatch [:db/set-in [::h] h]))))
                                 500)
        g-looper (js/setInterval #(do (println "RERUN G")
                                      (my-eval (cljs.reader/read-string (get-g))
                                               (fn [g] (rf/dispatch [:db/set-in [::g] g]))))
                                 500)
        render-mode (r/atom true)]
    (r/create-class
     {:component-will-unmount #(do (js/clearInterval h-looper)
                                   (js/clearInterval g-looper))
      :reagent-render
      (fn []
        [:div.hiccup-touch-panel
         {:class (when @render-mode "render-mode")}
         [:style (garden/css [:.hiccup-touch-panel #_{:all "initial"}
                              [:.* {:all "unset"}]])]
         [hiccup-editor]
         [garden-editor]
         (if @render-mode
           [render-result @h @g]
           [compile-result @h @g])
         [:div.switch-line
          [:div.switch
           [:label
            "Compiled"
            [:input {:type "checkbox"
                     :checked @render-mode
                     :on-change #(reset! render-mode (l "VAL:" (.-checked (.-target %))))}
             ]
            [:span.lever]
            "Rendered"]]]])})))
