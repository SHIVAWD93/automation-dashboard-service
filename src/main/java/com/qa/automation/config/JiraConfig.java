package com.qa.automation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import java.util.Base64;

@Configuration
public class JiraConfig {

    @Value("${jira.url:}")
    private String jiraUrl;

    @Value("${jira.username:}")
    private String jiraUsername;

    @Value("${jira.token:}")
    private String jiraToken;

    @Value("${jira.project.key:}")
    private String jiraProjectKey;

    @Value("${jira.board.id:}")
    private String jiraBoardId;

    @Bean
    public WebClient jiraWebClient() {
        // Increase memory limit for large Jira responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        return WebClient.builder()
                .baseUrl(jiraUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    public WebClient qtestWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    private String getBasicAuthHeader() {
        if (jiraUsername == null || jiraToken == null || 
            jiraUsername.isEmpty() || jiraToken.isEmpty()) {
            return "";
        }
        String credentials = jiraUsername + ":" + jiraToken;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // Getters for configuration values
    public String getJiraUrl() {
        return jiraUrl;
    }

    public String getJiraUsername() {
        return jiraUsername;
    }

    public String getJiraToken() {
        return jiraToken;
    }

    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public String getJiraBoardId() {
        return jiraBoardId;
    }

    public boolean isConfigured() {
        return jiraUrl != null && !jiraUrl.isEmpty() &&
               jiraUsername != null && !jiraUsername.isEmpty() &&
               jiraToken != null && !jiraToken.isEmpty();
    }
}