(ns exoscale.assessement
  (:require [aleph.http            :as http]
            [cheshire.core         :as json]
            [clojure.tools.logging :as log]
            [next.jdbc             :as jdbc]
            [next.jdbc.sql         :as sql]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:import [java.util Properties]
           [org.apache.kafka.clients.consumer ConsumerConfig KafkaConsumer]))

;; Kafka configuration
(def bootstrap-servers "Kafka server" "localhost:9092")
(def topics            "Kafka topics" ["topic.cloud.instances"])

(def instances-state (atom {}))

(defn create-consumer []
  (let [props (Properties.)]
    (.put props ConsumerConfig/GROUP_ID_CONFIG "exoscale")
    (.put props ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG bootstrap-servers)
    (.put props ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG "org.apache.kafka.common.serialization.StringDeserializer")
    (.put props ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG "org.apache.kafka.common.serialization.StringDeserializer")
    (KafkaConsumer. props)))

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

(defn seconds-between
  "Get the time differences between now and a given timestamp, in seconds"
  [timestamp-str]
  (let [formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ssZ")
        timestamp (f/parse formatter timestamp-str)
        duration (t/interval timestamp (t/now))]
    (t/in-seconds duration)))


(defn update-state
  "Update the running machines state, according to the given parsed event message"
  [event]
  (let [{:keys [payload]} event
        {:keys [before after op]} payload]
    (case op
      "c"
      (let [{:keys [organization_uuid instance_uuid price_second started]} after]
        (swap! instances-state assoc-in [organization_uuid instance_uuid]
               {:started started
                :price_second price_second
                :running true}))
      "d"
      (let [{:keys [organization_uuid instance_uuid price_second started]} before
            billing (* price_second (seconds-between started))
            current (get-in @instances-state [organization_uuid instance_uuid])]
        (if current
          (swap! instances-state assoc-in [organization_uuid instance_uuid]
                 {:billing billing
                  :running false}) current))
      nil)))


(defn start-usage-calculator
  "Starts a process that will calculate the usage based on a Kafka topic sourced
    from Change Data Capture."
  []
  (let [consumer (create-consumer)]
    (.subscribe consumer topics)
    (future
      (while true
        (let [records (.poll consumer 1000)]
          (when records
            (doseq [record records]
              (let [event (json/parse-string (.value record) true)]
                (update-state event)))))))))

(defn get-usage-summary
  "Get the usage summary of each organization's machines"
  [state]
  (reduce (fn [acc [org machines]]
            (let [started (count machines)
                  running (count (filter (fn [[_ m]] (= (:running m) true)) machines))
                  billing (reduce (fn [sum [_ m]] (+ sum (:billing m 0))) 0 machines)]
              (assoc acc org {:started started
                              :running running
                              :billing billing})))
          {}
          state))

(defn http-handler [req]
  (try
    {:status 200
     :headers {"content-type" "application/json"}
     :body (json/generate-string (get-usage-summary @instances-state))}
    (catch Exception e
      {:status 500
       :headers {"content-type" "text/plain"}
       :body (str "An error occurred: " (.getMessage e))})))

(defn start-server
  "Starts an HTTP server running on port 12000"
  []
  (http/start-server http-handler {:port 12000}))


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
