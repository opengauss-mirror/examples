package org.opengauss.example.login.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.List;
import org.opengauss.example.login.mapper.UserMapper;
import org.opengauss.example.login.model.UserInfo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;

  public UserServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
    if (StrUtil.isEmpty(s)) {
      throw new UsernameNotFoundException(s);
    }

    UserDetails userDetails = this.userMapper.get(s);

    if (ObjectUtil.isNull(userDetails)) {
      throw new UsernameNotFoundException(s);
    }

    return userDetails;
  }

  @Override
  public List<UserInfo> getList() {
    return this.userMapper.getList();
  }

  @Override
  public UserInfo get(String username) {
    return this.userMapper.get(username);
  }
}
