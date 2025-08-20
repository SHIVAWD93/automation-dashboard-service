package com.qa.automation.repository;

import com.qa.automation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User getUserByUserNameAndPassword(String userName, String password);

    User getUserByUserName(String username);
}
