package org.opengauss.example.login.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfiguration {

  @Bean
  public PasswordEncoder passwordEncoder() {
    //默认测试使用不加密方式
    return NoOpPasswordEncoder.getInstance();
  }
}
