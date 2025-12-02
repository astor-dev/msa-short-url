package com.naver.pay.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.shorturl.ShortUrl
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun redisCacheConfiguration(objectMapper: ObjectMapper): RedisCacheConfiguration {
        val jacksonSerializer = Jackson2JsonRedisSerializer(objectMapper, ShortUrl::class.java)
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jacksonSerializer)
            )
    }
}