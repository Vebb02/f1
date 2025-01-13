package no.vebb.f1.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import no.vebb.f1.scoring.UserScore;
import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;
import no.vebb.f1.util.Cutoff;
import no.vebb.f1.util.Table;

@Controller
@RequestMapping("/user")
public class ProfileController {

	private int year = 2025;

	@Autowired
	private UserService userService;

	@Autowired
	private Cutoff cutoff;
	
	private JdbcTemplate jdbcTemplate;

	public ProfileController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/{id}")
	public String guesserProfile(@PathVariable("id") UUID id, Model model) {
		Optional<User> optUser = userService.loadUser(id);
		if (optUser.isEmpty()) {
			return "redirect:/";
		}
		User user = optUser.get();
		return getGuesserProfile(model, user);
	}

	@GetMapping("/myprofile")
	public String myProfile(Model model) {
		Optional<User> optUser = userService.loadUser();
		if (optUser.isEmpty()) {
			return "redirect:/";
		}
		User user = optUser.get();
		return getGuesserProfile(model, user);
	}

	private String getGuesserProfile(Model model, User user) {
		if (!cutoff.isAbleToGuessCurrentYear() || userService.isLoggedInUser(user)) {
			UserScore userScore = new UserScore(user.id, year, jdbcTemplate);
			model.addAttribute("tables", userScore.getAllTables());
		} else {
			List<Table> tables = new ArrayList<>();
			String title = "Tippingen er tilgjenglig snart!";
			Table table = new Table(title, new ArrayList<>(), new ArrayList<>());
			tables.add(table);
			model.addAttribute("tables", tables);
		}
		
		model.addAttribute("title", user.username);
		model.addAttribute("loggedOut", !userService.isLoggedIn());
		return "profile";
	}

}
