package org.opengauss.example.login.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.opengauss.example.login.model.UserInfo;

@Mapper
public interface UserMapper {
  List<UserInfo> getList();

  UserInfo get(@Param("username") String username);

  void insert(UserInfo userInfo);
}
