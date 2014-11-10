(defproject fluxme-todo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.facebook/react "0.11.2"]
				 [datascript "0.5.1"]
				 [fluxme "0.1.0-SNAPSHOT"]
				 [org.clojure/clojure "1.6.0"]
				 [org.clojure/clojurescript "0.0-2371"]
				 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds
			  [{:id           "dev"
				:source-paths ["src"]
				:compiler     {:optimizations :whitespace
							   :pretty-print  true
							   :preamble      ["react/react.js"]
							   :output-to     "target/todo.js"}}]})
