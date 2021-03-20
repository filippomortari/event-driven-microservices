#!/bin/bash
# check-kafka-topics-created.sh

apt-get update -y

yes | apt-get install kafkacat curl

kafkacatResult=$(kafkacat -L -b kafka-broker-1:9092)

echo "kafkacat result:" $kafkacatResult

while [[ ! $curlResult == "200" ]]; do
  >&2 echo "Config server is not up yet!"
  sleep 2
  curlResult=$(curl -s -o /dev/null -I -w "%{http_code}" http://config-server:8888/actuator/health)
done

while [[ ! $kafkacatResult == *"twitter-topic"* ]]; do
  >&2 echo "Kafka topic has not been created yet!"
  sleep 2
  kafkacatResult=$(kafkacat -L -b kafka-broker-1:9092)
done

web