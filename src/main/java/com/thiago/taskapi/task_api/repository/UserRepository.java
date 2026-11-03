package com.thiago.taskapi.task_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.thiago.taskapi.task_api.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	Optional<User> findByEmail(String email);
	
	boolean existsByEmail(String Email);
	
	// O "hoje" é o de São Paulo, calculado pelo próprio PostgreSQL para não depender do fuso da JVM.
	@Query(value = """
			SELECT count(*) FROM users
			WHERE created_at >= date_trunc('day', now() AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'America/Sao_Paulo'
			""", nativeQuery = true)
	long countCreatedToday();
	
	// Lock de transação do PostgreSQL: cadastros concorrentes esperam aqui, então a contagem do
	// teto diário e o insert viram uma operação só. É liberado no commit/rollback.
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext('daily-signup-limit'))", nativeQuery = true)
	int acquireSignupLock();
	
	// O ON DELETE CASCADE do schema leva junto tarefas, categorias e tags do usuário.
	@Modifying
	@Query(value = "DELETE FROM users WHERE created_at < now() - interval '24 hours'", nativeQuery = true)
	int deleteOlderThan24Hours();
}
