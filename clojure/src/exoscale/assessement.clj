(ns exoscale.assessement
  (:require [aleph.http            :as http]
            [cheshire.core         :as json]
            [clojure.tools.logging :as log]
            [next.jdbc             :as jdbc]
            [next.jdbc.sql         :as sql])
  (:import java.util.Properties
            org.apache.kafka.clients.consumer.KafkaConsumer))

;; Kafka configuration
(def bootstrap-servers "Kafka server" "localhost:9092")
(def topics            "Kafka topics" ["topic.cloud.instances"])

;; MariaDB configuration
(def mariadb-user     "MariaDB user"     "root")
(def mariadb-password "MariaDB password" "debezium")

(def db-spec
  "Database credentials to connect to the MariaDB database to generate
  instances."
  {:dbtype "mariadb"
   :user mariadb-user
   :password mariadb-password
   :host "localhost"
   :dbname "cloud"})

(def organizations
  "Five organizations for which instances are going to be deployed."
  [#uuid "37edc843-926f-47ca-9a08-bcc0a54dffd8"
   #uuid "95f8a5e2-0807-448b-b2cd-102d78203c88"
   #uuid "74b23560-55b5-4e9e-9b51-a98097b817f6"
   #uuid "0969af61-3d3b-4896-bd13-673f1a840170"
   #uuid "ffb0ca88-123b-42f6-a2d2-b13f51dea292"])

(defn deploy-instance
  "Deploys an instance on the platform for an organization which a price between
  1 and 100 per second."
  []
  (sql/insert! db-spec
               :instances
               {:organization_uuid (first (shuffle organizations))
                :instance_uuid (random-uuid)
                :price_second (inc (rand-int 100))}))

(defn destroy-instance
  "Destroys a random instance."
  []
  (jdbc/execute! db-spec
                 ["DELETE FROM instances
                   WHERE id=(SELECT MIN(id) FROM instances)"]))
(defn simulator
  "Every 500 milliseconds, deploys a new instance or destroys a random one."
  []
  (loop []
    (Thread/sleep 500)
    ((first (shuffle [deploy-instance deploy-instance destroy-instance])))
    (recur)))

(defn start-simulator
  "Starts a process that is going to deploy and destroy instances to generate
  usage on the Exoscale platform."
  []
  (future (#'simulator)))

(defn start-usage-calculator
  "Starts a process that will calculate the usage based on a Kafka topic sourced
  from Change Data Capture.
  TODO: To be implemented"
  [])
  
(defn start-server
  "Starts an HTTP server running on port 12000
  TODO: To be implemented"
  [])

(defn start [& _]
  (log/info "Start simulator")
  (start-simulator)
  (log/info "Simulator started")
  (log/info "Starting usage calculator")
  (start-usage-calculator)
  (log/info "Usage calculator started")
  (log/info "Starting HTTP server")
  (start-server)
  (log/info "HTTP server started"))

(comment
  (start))
