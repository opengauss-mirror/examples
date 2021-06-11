package org.opengauss.example.login.service;

import java.util.List;
import org.opengauss.example.login.model.UserInfo;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

  List<UserInfo> getList();

  UserInfo get(String username);
}
