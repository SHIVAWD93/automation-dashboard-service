package com.qa.automation.service;

import com.qa.automation.model.UserPermission;
import com.qa.automation.repository.PermissionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;

    public List<UserPermission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}