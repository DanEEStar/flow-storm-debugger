.PHONY: install deploy clean help

clean:
	-rm flow-storm-debugger.jar
	-rm target -rf

pom.xml:
	clj -Spom
	mvn versions:set -DnewVersion=$(version)

watch-ui:
	npx shadow-cljs watch client

watch-css:
	clj -A:garden -e "(require '[garden-watcher.core :as gw]) (require '[com.stuartsierra.component :as component]) (component/start (gw/new-garden-watcher '[flow-storm-debugger.styles.main]))"

release-ui: clean
	npx shadow-cljs release client

flow-storm-debugger.jar: release-ui
	clj -A:jar flow-storm-debugger.jar

release: flow-storm-debugger.jar pom.xml

install: flow-storm-debugger.jar pom.xml
	mvn install:install-file -Dfile=flow-storm-debugger.jar -DpomFile=pom.xml

deploy:
	mvn deploy:deploy-file -Dfile=flow-storm-debugger.jar -DrepositoryId=clojars -DpomFile=pom.xml -Durl=https://clojars.org/repo

cider-repl:
	clj -Sdeps '{:deps {nrepl {:mvn/version "0.8.0"} refactor-nrepl {:mvn/version "2.5.0"} cider/cider-nrepl {:mvn/version "0.25.3"}}}' -m nrepl.cmdline --middleware '["refactor-nrepl.middleware/wrap-refactor", "cider.nrepl/cider-middleware"]'
run:
	clj -m flow-storm-debugger.server

tag-release:
	git add CHANGELOG.md && \
	git commit -m "Updating CHANGELOG after $(version) release" && \
	git tag "v$(version)" && \
	git push origin master

help:
	@echo "For creating a uberjar run"
	@echo "make version=x.y.z release"
	@echo "For releasing to clojars run"
	@echo "make deploy"
