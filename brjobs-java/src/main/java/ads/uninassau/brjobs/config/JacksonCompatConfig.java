package ads.uninassau.brjobs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCompatConfig {

    @Bean
    public ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }
}
