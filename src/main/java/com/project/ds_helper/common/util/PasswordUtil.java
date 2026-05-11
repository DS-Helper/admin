package com.project.ds_helper.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    public boolean isPasswordMatch(String password, String passwordCheck){
        if(password.isBlank() || passwordCheck.isBlank()){return false;}
        return password.equals(passwordCheck);
    }
}
