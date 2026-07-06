package ustin.cz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

@Slf4j
@EnableRetry
@EnableAsync
@Configuration
@EnableWebSocketMessageBroker
public class CZConfiguration implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/connect")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");  // <- Важно: префикс для @MessageMapping
        config.enableSimpleBroker("/queue", "/topic");
        config.setUserDestinationPrefix("/user");
    }

    @Bean(name = "taskExcelExecutor")
    public Executor taskQueueExecutor() {
        var executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("Virtual-Excel-");
        executor.setConcurrencyLimit(Integer.MAX_VALUE);

        log.info("Virtual Thread Executor инициализирован. Лимит: {}", executor.getConcurrencyLimit());

        return executor;
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
