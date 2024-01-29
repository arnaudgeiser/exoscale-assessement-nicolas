import logging
import mariadb
import json
from datetime import datetime
import random
import time
import sys
import uuid
from http.server import BaseHTTPRequestHandler, HTTPServer
import threading
from kafka import KafkaConsumer

logging.basicConfig(level=logging.INFO)

try:
    conn = mariadb.connect(
        user="root", password="debezium", host="127.0.0.1", port=3306, database="cloud"
    )
except mariadb.Error as e:
    print(f"Error connecting to MariaDB Platform: {e}")
    sys.exit(1)

organizations = [
    "37edc843-926f-47ca-9a08-bcc0a54dffd8",
    "95f8a5e2-0807-448b-b2cd-102d78203c88",
    "74b23560-55b5-4e9e-9b51-a98097b817f6",
    "0969af61-3d3b-4896-bd13-673f1a840170",
    "ffb0ca88-123b-42f6-a2d2-b13f51dea292",
]


def deploy_instance(cur):
    random.shuffle(organizations)
    cur.execute(
        "INSERT INTO instances (organization_uuid, instance_uuid, price_second) VALUES(?,?,?)",
        (organizations[0], str(uuid.uuid4()), random.randint(1, 100)),
    )


def destroy_instance(cur):
    cur.execute("DELETE FROM instances WHERE id=(SELECT MIN(id) FROM instances)")


def simulator():
    actions = [deploy_instance, deploy_instance, destroy_instance]
    while True:
        cur = conn.cursor()
        random.shuffle(actions)
        actions[0](cur)
        conn.commit()
        time.sleep(0.5)


def start_simulator():
    threading.Thread(target=simulator).start()


def start_usage_calculator():
    print("TODO")


def start_server():
    print("TODO")


if __name__ == "__main__":
    logging.info("Start simulator")
    start_simulator()
    logging.info("Simulator started")
    logging.info("Starting usage consumer")
    start_usage_calculator()
    logging.info("Usage consumer started")
    logging.info("Starting HTTP server")
    start_server()
    logging.info("HTTP server started")
