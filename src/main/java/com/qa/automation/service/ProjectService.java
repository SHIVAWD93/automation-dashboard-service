package com.qa.automation.service;

import com.qa.automation.model.Project;
import com.qa.automation.model.Domain;
import com.qa.automation.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DomainService domainService;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project createProject(Project project) {
        // Validate that domain exists
        if (project.getDomain() == null || project.getDomain().getId() == null) {
            throw new RuntimeException("Domain is required for creating a project");
        }

        Domain domain = domainService.getDomainById(project.getDomain().getId());
        if (domain == null) {
            throw new RuntimeException("Domain not found with id: " + project.getDomain().getId());
        }

        project.setDomain(domain);
        return projectRepository.save(project);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Project updateProject(Long id, Project project) {
        if (projectRepository.existsById(id)) {
            // Validate domain if provided
            if (project.getDomain() != null && project.getDomain().getId() != null) {
                Domain domain = domainService.getDomainById(project.getDomain().getId());
                if (domain == null) {
                    throw new RuntimeException("Domain not found with id: " + project.getDomain().getId());
                }
                project.setDomain(domain);
            }

            project.setId(id);
            return projectRepository.save(project);
        }
        return null;
    }

    public boolean deleteProject(Long id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Project> getProjectsByDomain(Long domainId) {
        return projectRepository.findByDomainId(domainId);
    }

    public List<Project> getProjectsByDomainAndStatus(Long domainId, String status) {
        return projectRepository.findByDomainIdAndStatus(domainId, status);
    }

    public List<Project> getProjectsByStatus(String status) {
        return projectRepository.findByStatus(status);
    }

    public long getProjectsCountByDomain(Long domainId) {
        return projectRepository.countByDomainId(domainId);
    }

    public long getProjectsCountByDomainAndStatus(Long domainId, String status) {
        return projectRepository.countByDomainIdAndStatus(domainId, status);
    }
}