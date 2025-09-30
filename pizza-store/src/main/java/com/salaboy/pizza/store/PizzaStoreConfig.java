package com.salaboy.pizza.store;

import io.dapr.spring.boot.autoconfigure.client.DaprClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({DaprClientProperties.class})
public class PizzaStoreConfig {
  @Bean
  RestTemplate restTemplate(){
    return new RestTemplate();
  }
}
