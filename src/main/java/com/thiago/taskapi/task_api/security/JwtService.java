package com.thiago.taskapi.task_api.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expiration;

	// Keys.hmacShaKeyFor lança WeakKeyException se o segredo tiver menos de 32 bytes,
	// então a aplicação nem sobe com uma chave fraca.
	public JwtService(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration}") long expiration) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiration = expiration;
	}

	public String generateToken(Long userId) {
		Date now = new Date();

		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiration))
				.signWith(key)
				.compact();
	}

	public Long extractUserId(String token) {
		String subject = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();

		return Long.valueOf(subject);
	}

	// O parse já verifica assinatura e expiração; qualquer falha cai no catch.
	public boolean isTokenValid(String token) {
		try {
			extractUserId(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}
