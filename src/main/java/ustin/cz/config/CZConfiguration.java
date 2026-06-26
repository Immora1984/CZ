package ustin.cz.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.exception.ExternalApiException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

@Slf4j
@EnableRetry
@EnableAsync
@Configuration
public class CZConfiguration {

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
