package com.nexusfuture.currency.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.nexusfuture.currency.repository",
        entityManagerFactoryRef = "sqliteEntityManagerFactory",
        transactionManagerRef = "sqliteTransactionManager"
)
@EntityScan(basePackages = "com.nexusfuture.currency.entity")
public class SqliteConfig {
    
    @Value("${spring.datasource.url}")
    private String sqliteUrl;
    
    @Value("${spring.datasource.driver-class-name}")
    private String sqliteDriver;
    
    @Bean(name = "sqliteDataSource")
    @Primary
    public DataSource sqliteDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(sqliteUrl);
        dataSource.setDriverClassName(sqliteDriver);
        return dataSource;
    }
    
    @Bean(name = "sqliteEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean sqliteEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(sqliteDataSource());
        em.setPackagesToScan("com.nexusfuture.currency.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.put("hibernate.show_sql", true);
        em.setJpaPropertyMap(properties);
        
        return em;
    }
    
    @Bean(name = "sqliteTransactionManager")
    @Primary
    public PlatformTransactionManager sqliteTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(sqliteEntityManagerFactory().getObject());
        return transactionManager;
    }
}
