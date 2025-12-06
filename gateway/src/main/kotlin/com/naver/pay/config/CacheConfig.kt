package com.naver.pay.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.naver.pay.util.createCommonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer


@Configuration
class CacheConfig {

    @Bean
    fun redisKeySerializer(): RedisSerializer<String> =
        StringRedisSerializer()

    @Bean
    fun redisValueSerializer(): RedisSerializer<Any> {
        val objectMapper = createCommonObjectMapper().apply {
            activateDefaultTyping(
                LaissezFaireSubTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        }
        return GenericJackson2JsonRedisSerializer(objectMapper)
    }

    @Bean
    fun redisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
        redisKeySerializer: RedisSerializer<String>,
        redisValueSerializer: RedisSerializer<Any>
    ): ReactiveRedisTemplate<String, Any> {
        val serializationContext = RedisSerializationContext.newSerializationContext<String, Any>()
            .key(redisKeySerializer)
            .value(redisValueSerializer)
            .hashKey(redisKeySerializer)
            .hashValue(redisValueSerializer)
            .build()
        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}