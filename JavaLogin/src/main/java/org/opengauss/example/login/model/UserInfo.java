package org.opengauss.example.login.model;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Date;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
public class UserInfo implements UserDetails {

  private String username;

  @JsonIgnore
  private String password;

  private String name;

  private boolean enabled;

  private Date createDate;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return ListUtil.empty();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
