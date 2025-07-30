package com.qa.automation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_id", nullable = false)
    @JsonIgnoreProperties("projects")
    private Domain domain;

    // NEW: Jira configuration fields
    @Column(name = "jira_project_key")
    private String jiraProjectKey;

    @Column(name = "jira_board_id")
    private String jiraBoardId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Project() {}

    public Project(String name, String description, String status, Domain domain) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.domain = domain;
    }

    public Project(String name, String description, String status, Domain domain,
                   String jiraProjectKey, String jiraBoardId) {
        this(name, description, status, domain);
        this.jiraProjectKey = jiraProjectKey;
        this.jiraBoardId = jiraBoardId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    // NEW: Jira configuration getters/setters
    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public void setJiraProjectKey(String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    public String getJiraBoardId() {
        return jiraBoardId;
    }

    public void setJiraBoardId(String jiraBoardId) {
        this.jiraBoardId = jiraBoardId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean hasJiraConfiguration() {
        return jiraProjectKey != null && !jiraProjectKey.trim().isEmpty() &&
                jiraBoardId != null && !jiraBoardId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", domain=" + (domain != null ? domain.getName() : "null") +
                ", jiraProjectKey='" + jiraProjectKey + '\'' +
                ", jiraBoardId='" + jiraBoardId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}