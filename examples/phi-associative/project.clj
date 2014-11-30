(defproject
  phi-associative "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [phi "0.5.0"]
                 [secretary "1.2.1"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:optimizations :whitespace
                           :pretty-print true
                           :preamble ["react/react.js"]
                           :output-to "target/todo.js"}}
               {:id "release"
                :source-paths ["src"]
                :compiler {:optimizations :advanced
                           :pretty-print false
                           :elide-asserts true
                           :preamble ["react/react.min.js"]
                           :externs ["react/externs/react.js"]
                           :output-to "target/todo.js"}}]})
