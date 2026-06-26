package ustin.cz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@SpringBootApplication
public class CzApplication {

    static void main(String[] args) {
        SpringApplication.run(CzApplication.class, args);
    }

}