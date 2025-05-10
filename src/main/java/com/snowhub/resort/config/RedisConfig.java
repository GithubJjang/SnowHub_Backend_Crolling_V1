package com.snowhub.resort.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String redisHost;
	@Value("${spring.data.redis.port}")
	private int redisPort;

	// 1. Host꺼
	@Bean
	@Qualifier("Master_redisConnectionFactory")
	public RedisConnectionFactory redisConnectionFactory(){
		return new LettuceConnectionFactory(redisHost, redisPort);
	}
	@Bean
	@Qualifier("Master_redisTemplate")
	public RedisTemplate<String, String> redisTemplate( @Qualifier("Master_redisConnectionFactory") RedisConnectionFactory connectionFactory) {

		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// 모든 경우
		template.setDefaultSerializer(new StringRedisSerializer());
		return template;
	}



}