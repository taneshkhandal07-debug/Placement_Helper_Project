package in.jecrcu.placement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}")
    private String rawUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:${DATABASE_USERNAME:}}")
    private String username;

    @Value("${SPRING_DATASOURCE_PASSWORD:${DATABASE_PASSWORD:}}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        if (rawUrl != null && !rawUrl.isBlank()) {
            String url = rawUrl.trim();
            // Handle postgres:// or postgresql:// URLs from Render/Neon/Supabase
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(url.replace("postgresql://", "postgres://"));
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        config.setUsername(parts[0]);
                        config.setPassword(parts[1]);
                    }
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        jdbcUrl += "?" + uri.getQuery();
                    }
                    config.setJdbcUrl(jdbcUrl);
                    config.setDriverClassName("org.postgresql.Driver");
                    log.info("Configured PostgreSQL DataSource: jdbc:postgresql://{}:{}{}", host, port, path);
                } catch (Exception e) {
                    log.error("Failed to parse PostgreSQL URL: {}, using raw value", e.getMessage());
                    config.setJdbcUrl(url);
                }
            } else {
                config.setJdbcUrl(url);
                if (username != null && !username.isBlank()) config.setUsername(username);
                if (password != null && !password.isBlank()) config.setPassword(password);
                if (url.startsWith("jdbc:h2:")) {
                    config.setDriverClassName("org.h2.Driver");
                } else if (url.startsWith("jdbc:postgresql:")) {
                    config.setDriverClassName("org.postgresql.Driver");
                }
            }
        } else {
            // Default: Embedded H2 database (Zero configuration required)
            log.info("No external database configured. Using embedded H2 database at ./data/youcan");
            config.setJdbcUrl("jdbc:h2:file:./data/youcan;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE");
            config.setDriverClassName("org.h2.Driver");
            config.setUsername("sa");
            config.setPassword("");
        }

        // Optimized pool settings for cloud containers (Render Free Tier 512MB RAM)
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
