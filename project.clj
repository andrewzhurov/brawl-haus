(defproject brawl-haus "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/core.async "0.4.474"]

                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/test "0.1.5"]
                 [healthsamurai/matcho "0.3.1"]

                 [re-com "2.1.0"]
                 [ns-tracker "0.3.1"]
                 [compojure "1.5.0"]
                 [ring "1.4.0"]
                 [re-pressed "0.2.2"]
                 [http-kit "2.2.0"]
                 [clj-time "0.14.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]

                 [paren-soup "2.13.2"]
                 [hiccups "0.3.0"]
                 [garden "1.3.5"]

                 [org.clojure/test.check "0.9.0" :scope "test"]

                 [thi.ng/geom "0.0.908"]
                 #_[thi.ng/geom-svg "0.0.908"]
                 [thi.ng/ndarray "0.3.0"]
                 [thi.ng/strf "0.2.1"]
                 [thi.ng/xerror "0.1.0"]
                 [thi.ng/color "1.0.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-garden "0.3.0"]
            [lein-cljfmt "0.6.0"]
            [lein-ancient "0.6.15"]
            ;; left clojurescript.test out
            ]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test" "test/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler brawl-haus.handler/dev-handler}

  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   brawl-haus.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :repl-options {:timeout 600000
                 :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}

  :aliases {"dev" ["do" "clean"
                   ["pdo" ["figwheel" "dev"]
                    ["garden" "auto"]]]
            "build" ["with-profile" "+prod,-dev" "do"
                     ["clean"]
                     ["cljsbuild" "once" "min"]
                     ["garden" "once"]]
            "stage" ["with-profile" "stage" "do"
                     ["clean"]
                     ["cljsbuild" "once" "min"]
                     ["garden" "once"]
                     ["compile"]]}

  :profiles
  {:dev
   {:source-paths ["dev"]
    :dependencies [[binaryage/devtools "0.9.10"]
                   
                   
                   [day8.re-frame/re-frame-10x "0.3.3"]
                   [day8.re-frame/tracing "0.5.1"]
                   [figwheel-sidecar "0.5.16"]
                   [cider/piggieback "0.3.5"]

                   [criterium "0.4.3"]
                   [tentacles "0.3.0"]]

    :plugins       [[com.jakemccrary/lein-test-refresh "0.23.0"]
                    [cider/cider-nrepl "0.21.1"]
                    [lein-figwheel "0.5.16"]
                    [lein-pdo "0.1.1"]]}
   :stage ;; dev.brawl.haus
   {:source-paths ["prod"]
    :dependencies [[binaryage/devtools "0.9.10"]
                   [day8.re-frame/re-frame-10x "0.3.3"]
                   [day8.re-frame/tracing "0.5.1"]
                   [figwheel-sidecar "0.5.16"]
                   [cider/piggieback "0.3.5"]]

    :plugins      [[lein-figwheel "0.5.16"]
                   [lein-pdo "0.1.1"]]}

   :prod
   {:source-paths ["prod"]
    :dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "brawl-haus.core/mount-root"}
     :compiler     {:main                 brawl-haus.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true
                                           "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            brawl-haus.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}

  :main brawl-haus.server

  :aot [brawl-haus.server]

  :uberjar-name "brawl-haus.jar"

  ;:prep-tasks [["cljsbuild" "once" "min"]["garden" "once"] "compile"]
  )
