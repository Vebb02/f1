package no.vebb.f1.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;

@Controller
@RequestMapping("/settings")
public class UserSettingsController {
	
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserService userService;

	private final String usernameUrl = "/settings/username";

	public UserSettingsController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping()
	public String settings(Model model) {
		model.addAttribute("title", "Innstillinger");
		Map<String, String> linkMap = new LinkedHashMap<>();
		linkMap.put("Endre brukernavn", "/settings/username");
		linkMap.put("Slett bruker", "/settings/delete");
		model.addAttribute("linkMap", linkMap);
		return "linkList";
	}

	@GetMapping("/username")
	public String changeUsername(Model model) {
		model.addAttribute("url", usernameUrl);
		return "registerUsername";
	}

	@PostMapping("/username")
	public String changeUsername(String username, Model model) {
		username = username.strip();
		UsernameController usernameController = new UsernameController(jdbcTemplate);
		String error = usernameController.validateUsername(username);
		if (error != null) {
			model.addAttribute("error", error);
			model.addAttribute("url", usernameUrl);
			return "registerUsername";
		}
		final String updateUsername = """
			UPDATE User
			SET username = ?, username_upper = ?
			WHERE id = ?
			""";
		String usernameUpper = username.toUpperCase();
		final UUID id = userService.loadUser().get().id;

		jdbcTemplate.update(updateUsername, username, usernameUpper, id);
		return "redirect:/settings";
	}

	@GetMapping("/delete")
	public String deleteAccount(Model model) {
		String username = userService.loadUser().get().username;
		model.addAttribute("username", username);
		
		return "deleteAccount";
	}
	
	@PostMapping("/delete")
	public String deleteAccount(Model model, @RequestParam("username") String username, HttpServletRequest request) {
		User user = userService.loadUser().get();
		String actualUsername = user.username;
		if (!username.equals(actualUsername)) {
			model.addAttribute("username", actualUsername);
			model.addAttribute("error", "Brukernavn er feil");
			return "deleteAccount";
		}
		final String deleteUser = """
			UPDATE User
			SET username = 'Anonym', username_upper = 'ANONYM', google_id = ?
			WHERE id = ?
			""";
		UUID id = user.id;
		jdbcTemplate.update(deleteUser, id, id);

		request.getSession().invalidate();
		SecurityContextHolder.clearContext();

		return "redirect:/";
	}

}
