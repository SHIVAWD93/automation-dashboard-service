package com.qa.automation.repository;

import com.qa.automation.model.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<UserPermission, Long> {
    UserPermission findUserPermissionByPermission(String permission);
}
