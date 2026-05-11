package com.project.ds_helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DsHelperApplication {

	public static void main(String[] args) {
		SpringApplication.run(DsHelperApplication.class, args);
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

}
