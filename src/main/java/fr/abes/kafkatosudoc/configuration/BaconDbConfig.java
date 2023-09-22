package fr.abes.kafkatosudoc.configuration;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "baconEntityManager", transactionManagerRef = "baconTransactionManager", basePackages = "fr.abes.kafkatosudoc.repository")
@NoArgsConstructor
@BaconDbConfiguration
public class BaconDbConfig {
    @Value("${spring.jpa.bacon.show-sql}")
    protected boolean showsql;
    @Value("${spring.jpa.bacon.properties.hibernate.dialect}")
    protected String dialect;
    @Value("${spring.jpa.bacon.hibernate.ddl-auto}")
    protected String ddlAuto;
    @Value("${spring.jpa.bacon.database-platform}")
    protected String platform;
    @Value("${spring.jpa.bacon.generate-ddl}")
    protected boolean generateDdl;
    @Value("${spring.sql.bacon.init.mode}")
    protected String initMode;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.bacon")
    public DataSource baconDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    public LocalContainerEntityManagerFactoryBean baconEntityManager() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(baconDataSource());
        em.setPackagesToScan(new String[]{"fr.abes.kafkatosudoc.entity"});
        HibernateJpaVendorAdapter vendorAdapter
                = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(generateDdl);
        vendorAdapter.setShowSql(showsql);
        vendorAdapter.setDatabasePlatform(platform);
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.dialect", dialect);
        properties.put("logging.level.org.hibernate", "DEBUG");
        properties.put("hibernate.type", "trace");
        properties.put("spring.sql.init.mode", initMode);
        em.setJpaPropertyMap(properties);
        return em;
    }

    @Primary
    @Bean
    public PlatformTransactionManager baconTransactionManager(@Qualifier("baconEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

}
