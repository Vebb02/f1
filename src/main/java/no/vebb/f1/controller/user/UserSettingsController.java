package no.vebb.f1.controller.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import no.vebb.f1.database.Database;
import no.vebb.f1.user.User;
import no.vebb.f1.user.UserMail;
import no.vebb.f1.user.UserMailService;
import no.vebb.f1.user.UserService;
import no.vebb.f1.util.collection.Table;
import no.vebb.f1.util.domainPrimitive.MailOption;
import no.vebb.f1.util.domainPrimitive.Username;
import no.vebb.f1.util.exception.InvalidEmailException;
import no.vebb.f1.util.exception.InvalidUsernameException;

/**
 * Class is responsible for managing the user settings. Like changing username
 * and deleting user.
 */
@Controller
@RequestMapping("/settings")
public class UserSettingsController {

	private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);

	@Autowired
	private Database db;

	@Autowired
	private UserService userService;

	@Autowired
	private UserMailService userMailService;

	private final String usernameUrl = "/settings/username";

	/**
	 * Handles GET requests for /settings. Gives a list of links to further navigate
	 * the settings.
	 */
	@GetMapping
	public String settings(Model model) {
		model.addAttribute("title", "Innstillinger");
		Map<String, String> linkMap = new LinkedHashMap<>();
		linkMap.put("Se brukerinformasjon", "/settings/info");
		linkMap.put("Påminnelser", "/settings/mail");
		linkMap.put("Inviter brukere", "/settings/referral");
		linkMap.put("Endre brukernavn", "/settings/username");
		linkMap.put("Slett bruker", "/settings/delete");
		model.addAttribute("linkMap", linkMap);
		return "util/linkList";
	}

	/**
	 * Handles GET requests for /settings/info. Gives the user information about
	 * their username, user ID and Google ID that is associated with their user.
	 */
	@GetMapping("/info")
	public String userInformation(Model model) {
		model.addAttribute("title", "Brukerinformasjon");
		List<Table> tables = new ArrayList<>();
		User user = userService.loadUser().get();
		tables.add(new Table("", Arrays.asList("Brukernavn"), Arrays.asList(Arrays.asList(user.username))));
		tables.add(new Table("", Arrays.asList("Bruker-ID"), Arrays.asList(Arrays.asList(user.id.toString()))));
		tables.add(new Table("", Arrays.asList("Google-ID"), Arrays.asList(Arrays.asList(user.googleId.toString()))));
		try {
			tables.add(new Table("", Arrays.asList("E-post"), Arrays.asList(Arrays.asList(db.getEmail(user.id)))));
		} catch (EmptyResultDataAccessException e) {
		}
		tables.add(new Table("Tippet sjåfør", Arrays.asList("Plass", "Sjåfør", "År"), db.userGuessDataDriver(user.id)));
		tables.add(new Table("Tippet konstruktør", Arrays.asList("Plass", "Konstruktør", "År"), db.userGuessDataConstructor(user.id)));
		tables.add(new Table("Tippet antall", Arrays.asList("Type", "Tippet", "År"), db.userGuessDataFlag(user.id)));
		tables.add(new Table("Tippet løp", Arrays.asList("Type", "Tippet", "Løp", "År"), db.userGuessDataDriverPlace(user.id)));
		tables.add(new Table("Påminnelser e-post", Arrays.asList("Løp", "Antall påminnelser", "År"), db.userDataNotified(user.id)));
		tables.add(new Table("Preferanser påminnelser", Arrays.asList("Timer før løp"),
			db.getMailingPreference(user.id).stream()
				.map(option -> Arrays.asList(option.toString()))
				.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
					Collections.reverse(list);
					return list;
				}))));
		model.addAttribute("tables", tables);
		return "util/tables";
	}

	/**
	 * Handles GET requests for /settings/username. Gives the form for changing
	 * username.
	 */
	@GetMapping("/username")
	public String changeUsername(Model model) {
		model.addAttribute("url", usernameUrl);
		return "user/registerUsername";
	}

	/**
	 * Handles POST requests for /settings/username. If the username is valid, it
	 * changes the username in the database. Otherwise, it gives an error message to
	 * the user.
	 */
	@PostMapping("/username")
	@Transactional
	public String changeUsername(String username, Model model) {
		try {
			Username validUsername = new Username(username, db);
			final UUID id = userService.loadUser().get().id;
			db.updateUsername(validUsername, id);
		} catch (InvalidUsernameException e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("url", usernameUrl);
			return "user/registerUsername";
		}

		return "redirect:/settings";
	}

	/**
	 * Handles GET requests for /settings/delete. Gives a form to confirm deletion
	 * of account.
	 */
	@GetMapping("/delete")
	public String deleteAccount(Model model) {
		String username = userService.loadUser().get().username;
		model.addAttribute("username", username);

		return "user/deleteAccount";
	}

	/**
	 * Handles POST requests for /settings/delete. If the input username matches the
	 * username of the user the user is anonymized and Google ID removed. This
	 * revokes their access to the website. If the username is incorrect, the user
	 * gets an error message.
	 */
	@PostMapping("/delete")
	@Transactional
	public String deleteAccount(Model model, @RequestParam("username") String username, HttpServletRequest request) {
		User user = userService.loadUser().get();
		String actualUsername = user.username;
		if (!username.equals(actualUsername)) {
			model.addAttribute("username", actualUsername);
			model.addAttribute("error", "Brukernavn er feil");
			return "user/deleteAccount";
		}
		db.deleteUser(user.id);

		request.getSession().invalidate();
		SecurityContextHolder.clearContext();

		return "redirect:/";
	}

	@GetMapping("/mail")
	public String mailingList(Model model) {
		User user = userService.loadUser().get();
		boolean hasMail = db.userHasEmail(user.id);
		model.addAttribute("hasMail", hasMail);
		if (hasMail) {
			Map<Integer, Boolean> mailOptions = new LinkedHashMap<>();
			model.addAttribute("mailOptions", mailOptions);
			List<MailOption> options = db.getMailingOptions();
			for (MailOption option : options) {
				mailOptions.put(option.value, false);
			}
			List<MailOption> preferences = db.getMailingPreference(user.id);
			for (MailOption preference : preferences) {
				mailOptions.put(preference.value, true);
			}
		}
		return "user/mail";
	}

	@PostMapping("/mail/add")
	@Transactional
	public String addMailingList(Model model, @RequestParam("email") String email) {
		try {
			User user = userService.loadUser().get();
			UserMail userMail = new UserMail(user, email);
			userMailService.sendVerificationCode(userMail);
			return "redirect:/settings/mail/verification";
		} catch (InvalidEmailException e) {
			return "redirect:/settings/mail";
		}
	}
	
	@PostMapping("/mail/remove")
	@Transactional
	public String removeMailingList(Model model) {
		User user = userService.loadUser().get();
		db.clearUserFromMailing(user.id);
		return "redirect:/settings/mail";
	}

	@GetMapping("/mail/verification")
	public String verificationCodeForm() {
		User user = userService.loadUser().get();
		if (!db.hasVerificationCode(user.id)) {
			return "redirect:/settings/mail";
		}
		return "user/verificationCode";
	}

	@PostMapping("/mail/verification")
	@Transactional
	public String verificationCode(Model model, @RequestParam("code") int code, HttpServletRequest request) {
		User user = userService.loadUser().get();
		boolean isValidVerificationCode = db.isValidVerificationCode(user.id, code);
		if (isValidVerificationCode) {
			logger.info("Successfully verified email of user '{}'", user.id);
			return "redirect:/settings/mail";
		}
		logger.warn("User '{}' put the wrong verification code", user.id);
		
		return "redirect:/settings/mail/verification";
	}

	@PostMapping("/mail/option/add")
	@Transactional
	public String addMailingOption(@RequestParam("option") int option) {
		try {
			User user = userService.loadUser().get();
			MailOption mailOption = new MailOption(option, db);
			db.addMailOption(user.id, mailOption);
		} catch (InvalidEmailException e) {
		}
		return "redirect:/settings/mail";
	}
	
	@PostMapping("/mail/option/remove")
	@Transactional
	public String removeMailingOption(@RequestParam("option") int option) {
		try {
			User user = userService.loadUser().get();
			MailOption mailOption = new MailOption(option, db);
			db.removeMailOption(user.id, mailOption);
		} catch (InvalidEmailException e) {
		}
		return "redirect:/settings/mail";
	}

	@GetMapping("/referral")
	public String generateReferralCodeForm(Model model) {
		UUID userId = userService.loadUser().get().id;
		Long referralCode = db.getReferralCode(userId);
		if (referralCode != null) {
			model.addAttribute("referralCode", referralCode);
		}
		return "user/referralCode";
	}
	
	@PostMapping("/referral/add")
	@Transactional
	public String generateReferralCode() {
		UUID userId = userService.loadUser().get().id;
		db.addReferralCode(userId);
		return "redirect:/settings/referral";
	}
	
	@PostMapping("/referral/delete")
	@Transactional
	public String removeReferralCode() {
		UUID userId = userService.loadUser().get().id;
		db.removeReferralCode(userId);
		return "redirect:/settings/referral";
	}
}
