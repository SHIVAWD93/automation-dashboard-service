package com.qa.automation.service;

import com.qa.automation.dto.UserDto;
import com.qa.automation.model.User;
import com.qa.automation.model.UserPermission;
import com.qa.automation.repository.PermissionRepository;
import com.qa.automation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    AuthenticationManager authManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private JWTService jwtService;

    public UserDto getUserDetails(String userName, String password) {
        User user = userRepository.getUserByUserNameAndPassword(userName, password);
        String token = this.verify(user);
        UserDto userDto = new UserDto(user.getUserName(), user.getPassword(), user.getCreatedAt(), user.getRole(), user.getUpdatedAt(), user.getUserPermissions().getId());
        userDto.setToken(token);
        userDto.setPermission(user.getUserPermissions().getPermission());
        return userDto;
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

//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        return this.userRepository.getUserByUserName(username);
//    }

    public String verify(User user) {
            return jwtService.generateToken(user.getUserName());
    }
}
