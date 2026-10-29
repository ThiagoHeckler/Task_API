package com.thiago.taskapi.task_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thiago.taskapi.task_api.repository.UserRepository;

// Toda conta é de demonstração: a cada hora, apaga as criadas há mais de 24 h.
@Component
public class DemoAccountCleanup {
	
	private static final Logger log = LoggerFactory.getLogger(DemoAccountCleanup.class);
	
	private final UserRepository userRepository;
	
	public DemoAccountCleanup(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void deleteExpiredAccounts() {
		int deleted = userRepository.deleteOlderThan24Hours();
		if (deleted > 0) {
			log.info("Contas demo apagadas: {}", deleted);
		}
	}
}
