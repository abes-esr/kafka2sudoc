package fr.abes.kafkatosudoc.configuration;

import fr.abes.cbs.process.ProcessCBS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public ProcessCBS processCBS() {
        return new ProcessCBS();
    }
}
