package com.qa.automation.service;

import com.qa.automation.dto.UserDto;
import com.qa.automation.model.User;
import com.qa.automation.model.UserPermission;
import com.qa.automation.repository.PermissionRepository;
import com.qa.automation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionRepository permissionRepository;

    public User getUserDetails(String userName, String password) {
        return userRepository.getUserByUserNameAndPassword(userName, password);
    }

    public User saveUser(UserDto user) {
        Long permissionId = user != null ? user.getUserPermission() : null;

        if (permissionId == null) {
            UserPermission readPermission = this.permissionRepository.findUserPermissionByPermission("read");
            permissionId = readPermission.getId();
        }
        UserPermission permission = permissionRepository.findById(permissionId).get();
        User updatedUser = new User(user.getUserName(), user.getPassword(), user.getRole(), permission);
        return userRepository.save(updatedUser);
    }


    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
