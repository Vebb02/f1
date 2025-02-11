package no.vebb.f1.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import no.vebb.f1.user.UserService;

@Controller
@RequestMapping("/username")
public class UsernameController {

	private static final Logger logger = LoggerFactory.getLogger(UsernameController.class);

	private final JdbcTemplate jdbcTemplate;

	private final String url = "/username";


	@Autowired
	private UserService userService;

	public UsernameController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping
	public String registerUsernameForm(Model model) {
		if (userService.isLoggedIn()) {
			return "redirect:/";
		}
		model.addAttribute("url", url);
		return "registerUsername";
	}

	@PostMapping
	public String registerUsername(@AuthenticationPrincipal OAuth2User principal,
			@RequestParam("username") String username,
			Model model) {
		final String googleId = principal.getName();

		username = username.strip();

		String error = validateUsername(username);

		if (error != null) {
			model.addAttribute("error", error);
			model.addAttribute("url", url);
			return "registerUsername";
		}

		String username_upper = username.toUpperCase();
		final String sqlInsertUsername = "INSERT INTO User (google_id, id,username, username_upper) VALUES (?, ?, ?, ?)";
		try {
			jdbcTemplate.update(sqlInsertUsername, googleId, UUID.randomUUID(), username, username_upper);
		} catch (DataAccessException e) {
			// Try again to ensure it could not be equal UUID
			logger.warn("Failed to set UUID to new user. Tried again.");
			jdbcTemplate.update(sqlInsertUsername, googleId, UUID.randomUUID(), username, username_upper);
		}
		return "redirect:/";
	}

	public String validateUsername(String username) {
		if (username.equals("")) {
			return "Brukernavn kan ikke være blankt.";
		}

		if (!username.matches("^[a-zA-ZÆØÅæøå ]+$")) {
			return "Brukernavn kan bare inneholde (a-å, A-Å).";
		}

		String username_upper = username.toUpperCase();
		
		if (username_upper.equals("ANONYM")) {
			return "Brukernavn kan ikke være 'Anonym'";
		}

		final String sqlCheckUsername = "SELECT COUNT(*) FROM User WHERE username_upper = ?";
		Integer usernameCount = jdbcTemplate.queryForObject(sqlCheckUsername, Integer.class, username_upper);

		if (usernameCount != null && usernameCount > 0) {
			return "Brukernavnet er allerede i bruk. Vennligst velg et annet.";
		}
		
		return null;
	}
}
