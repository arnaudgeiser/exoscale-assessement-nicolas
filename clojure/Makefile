.PHONY: *
.DEFAULT_GOAL:=help

# Internal
ROOT_DIR=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
VERSION=$(shell cat $(ROOT_DIR)/VERSION)
CLJ=clojure -J-Dclojure.main.report=stderr

##@ Commands

run: ## Start application
	clojure -X exoscale.assessement/start

compose-up: ## Start containers
	echo "Starting containers"
	docker compose up --build -d --remove-orphans
	echo "Containers started"
	echo "Waiting 20 seconds to ensure all the containers are started (could be improved...)"
	sleep 20
	echo "Registering connector"
	curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "cloud-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mariadb", "database.port": "3306", "database.user": "root", "database.password": "debezium", "database.server.id": "184054", "topic.prefix": "topic", "database.include.list": "cloud", "schema.history.internal.kafka.bootstrap.servers": "kafka:29092", "schema.history.internal.kafka.topic": "schemahistory.cloud" } }'
	echo "Connector registered"

compose-down: ## Stop containers
	docker-compose down --remove-orphans

help:  ## Display this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m\033[0m\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)


