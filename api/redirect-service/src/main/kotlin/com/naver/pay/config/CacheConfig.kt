package com.naver.pay.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.naver.pay.util.getCommonJacksonModules
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration


@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun redisKeySerializer(): RedisSerializer<String> =
        StringRedisSerializer()

    @Bean
    fun redisValueSerializer(): RedisSerializer<Any> {
        val objectMapper = ObjectMapper().apply {
            getCommonJacksonModules().forEach { module ->
                registerModule(module)
            }
            activateDefaultTyping(
                LaissezFaireSubTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        }
        return GenericJackson2JsonRedisSerializer(objectMapper)
    }

    @Bean
    fun redisCacheConfiguration(
        redisKeySerializer: RedisSerializer<String>,
        redisValueSerializer: RedisSerializer<Any>
    ): RedisCacheConfiguration {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisKeySerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer))
            .entryTtl(Duration.ofMinutes(10))
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        redisKeySerializer: RedisSerializer<String>,
        redisValueSerializer: RedisSerializer<Any>
    ): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = redisKeySerializer
            valueSerializer = redisValueSerializer
            hashKeySerializer = redisKeySerializer
            hashValueSerializer = redisValueSerializer
            afterPropertiesSet()
        }
    }
}