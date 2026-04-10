package com.carnetroute.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

object KafkaConfig {

    val BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"

    const val TOPIC_FUEL_PRICES = "carnetroute.fuel.prices"
    const val TOPIC_FUEL_ALERTS = "carnetroute.fuel.alerts"
    const val CONSUMER_GROUP = "carnetroute-group"

    fun producerProperties(): Properties = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.RETRIES_CONFIG, 3)
        put(ProducerConfig.LINGER_MS_CONFIG, 10)
    }

    fun consumerProperties(): Properties = Properties().apply {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
        put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
        put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    }
}
