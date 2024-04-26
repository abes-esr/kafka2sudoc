package fr.abes.kafkatosudoc.configuration;

import fr.abes.cbs.process.ProcessCBS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class Config {

    @Bean
    public ProcessCBS processCBS() {
        return new ProcessCBS();
    }
}
