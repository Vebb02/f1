package no.vebb.f1.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final JdbcTemplate jdbcTemplate;

	public UserService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<User> loadUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return Optional.empty();
		}
		final String googleId = authentication.getName();
		final String sql = "SELECT username, id FROM User WHERE google_id = ?";
		try {
			return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
				String username = rs.getString("username");
				UUID id = UUID.fromString(rs.getString("id"));
				return Optional.of(new User(googleId, id, username));
			}, googleId);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
}
