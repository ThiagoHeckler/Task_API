package com.thiago.taskapi.task_api;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableScheduling
public class TaskApiApplication {

	// Fuso único para JVM e sessão do PostgreSQL (o driver usa o fuso da JVM): created_at é TIMESTAMP
	// sem fuso preenchido pelo now() da sessão, e dev (São Paulo) e container (UTC) gravariam horas
	// diferentes. Bloco estático para valer antes de qualquer conexão, inclusive nos testes.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TaskApiApplication.class, args);
	}

}
