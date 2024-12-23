package no.vebb.f1.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;

@RestController
public class UserInformationController {
	
	@Autowired
    private UserService userService;


	@GetMapping("/id")
	public String id() {
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "No id found";
		}
		UUID id = user.get().getId();
		return id.toString();
	}

	@GetMapping("/googleid")
	public String googleId() {
		Optional<User> user = userService.loadUser();
		String id = user.map(User::getGoogleId).orElse("No id found");
		return id.toString();
	}

	@GetMapping("/name")
	public String name() {
		Optional<User> user = userService.loadUser();
		String username = user.map(User::getUsername).orElse("No username found");
		return username;
	}

}
