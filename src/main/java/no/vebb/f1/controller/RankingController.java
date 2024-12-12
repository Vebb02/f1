package no.vebb.f1.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;

@Controller
@RequestMapping("/guess")
public class RankingController {

	private JdbcTemplate jdbcTemplate;
	private static final Logger logger = LoggerFactory.getLogger(RankingController.class);

	@Autowired
	private UserService userService;

	private int year = 2024;

	public RankingController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping()
	public String guess(@RequestParam(value = "success", required = false) Boolean success, Model model) {
		if (success != null) {
			if (success) {
				model.addAttribute("successMessage", "Tippingen din ble lagret");
			} else {
				model.addAttribute("successMessage",
						"Tippingen feilet, vennligst prøv igjen eller kontakt administrator");
			}
		}
		return "guessMenu";
	}

	@GetMapping("/drivers")
	public String rankDrivers(Model model) {
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		String sql = "SELECT name FROM Driver";
		String guessedSql = "SELECT position, driver FROM DriverGuess where guesser = ?";
		List<Map<String, Object>> guessed = jdbcTemplate.queryForList(guessedSql, user.get().id);
		List<String> drivers;
		if (guessed.size() == 0) {
			drivers = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"));
		} else {
			drivers = new ArrayList<>();
			List<PositionedItem> driversWithPos = new ArrayList<>();
			for (Map<String, Object> row : guessed) {
				int pos = (int) row.get("position");
				String driver = (String) row.get("driver");
				driversWithPos.add(new PositionedItem(pos, driver));
			}
			Collections.sort(driversWithPos);
			driversWithPos.forEach(item -> drivers.add(item.value));
		}
		model.addAttribute("items", drivers);
		model.addAttribute("title", "Ranger sjåførene");
		model.addAttribute("type", "drivers");
		return "ranking";
	}

	@PostMapping("/drivers")
	public String rankDrivers(@RequestParam List<String> rankedItems, Model model) {
		String sql = "SELECT name FROM Driver";
		Set<String> drivers = new HashSet<>((jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"))));
		String error = validateGuessList(rankedItems, drivers);
		if (error != null) {
			logger.warn(error);
			return "redirect:/guess?success=false";
		}
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		int position = 1;
		for (String driver : rankedItems) {
			final String addRowDriver = "REPLACE INTO DriverGuess (guesser, driver, year, position) values (?, ?, ?, ?)";
			jdbcTemplate.update(addRowDriver, user.get().id, driver, year, position);
			position++;
		}
		return "redirect:/guess?success=true";
	}

	@GetMapping("/constructors")
	public String rankConstructors(Model model) {
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		String sql = "SELECT name FROM Constructor";
		String guessedSql = "SELECT position, constructor FROM ConstructorGuess where guesser = ?";
		List<Map<String, Object>> guessed = jdbcTemplate.queryForList(guessedSql, user.get().id);
		List<String> constructors;
		if (guessed.size() == 0) {
			constructors = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"));
		} else {
			constructors = new ArrayList<>();
			List<PositionedItem> constructorWithPos = new ArrayList<>();
			for (Map<String, Object> row : guessed) {
				int pos = (int) row.get("position");
				String constructor = (String) row.get("constructor");
				constructorWithPos.add(new PositionedItem(pos, constructor));
			}
			Collections.sort(constructorWithPos);
			constructorWithPos.forEach(item -> constructors.add(item.value));
		}
		model.addAttribute("items", constructors);
		model.addAttribute("title", "Ranger konstruktørene");
		model.addAttribute("type", "constructors");
		return "ranking";
	}

	@PostMapping("/constructors")
	public String rankConstructors(@RequestParam List<String> rankedItems, Model model) {
		final String getConstructorSql = "SELECT name FROM Constructor";
		Set<String> contructors = new HashSet<>((jdbcTemplate.query(getConstructorSql, (rs, rowNum) -> rs.getString("name"))));
		String error = validateGuessList(rankedItems, contructors);
		if (error != null) {
			logger.warn(error);
			return "redirect:/guess?success=false";
		}
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		int position = 1;
		for (String constructor : rankedItems) {
			final String addRowConstructor = "REPLACE INTO ConstructorGuess (guesser, constructor, year, position) values (?, ?, ?, ?)";
			jdbcTemplate.update(addRowConstructor, user.get().id, constructor, year, position);
			position++;
		}
		return "redirect:/guess?success=true";
	}

	private String validateGuessList(List<String> guessed, Set<String> original) {
		Set<String> guessedSet = new HashSet<>();
		for (String competitor : guessed) {
			if (!original.contains(competitor)) {
				return String.format("%s is not a valid competitor.", competitor);
			}
			if (!guessedSet.add(competitor)) {
				return String.format("Competitor %s is guessed twice in the ranking.", competitor);
			}
		}
		if (original.size() != guessedSet.size()) {
			return String.format("Not all competitors are guessed. Expected %d, was %d.", original.size(),
					guessedSet.size());
		}
		return null;
	}

	@GetMapping("/tenth")
	public String guessTenth(Model model) {
		String sql = "SELECT name FROM Driver";
		List<String> drivers = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"));
		model.addAttribute("items", drivers);
		model.addAttribute("title", "Tipp 10.plass");
		model.addAttribute("type", "tenth");
		return "chooseDriver";
	}

	@PostMapping("/tenth")
	public String guessTenth(@RequestParam String driver, Model model) {
		String sql = "SELECT name FROM Driver";
		Set<String> driversCheck = new HashSet<>((jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"))));
		if (!driversCheck.contains(driver)) {
			logger.warn("'{}' invalid tenth driver inputted by user.", driver);
			return "redirect:/guess?success=false";
		}
		logger.info("Guessed '{}' on tenth", driver);
		return "redirect:/guess?success=true";
	}

	@GetMapping("/winner")
	public String guessWinner(Model model) {
		String sql = "SELECT name FROM Driver";
		List<String> drivers = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"));
		model.addAttribute("items", drivers);
		model.addAttribute("title", "Tipp Vinneren");
		model.addAttribute("type", "winner");
		return "chooseDriver";
	}

	@PostMapping("/winner")
	public String guessWinner(@RequestParam String driver, Model model) {
		String sql = "SELECT name FROM Driver";
		Set<String> driversCheck = new HashSet<>((jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"))));
		if (!driversCheck.contains(driver)) {
			logger.warn("'{}', invalid winner driver inputted by user.", driver);
			return "redirect:/guess?success=false";
		}
		logger.info("Guessed '{}' as winner", driver);
		return "redirect:/guess?success=true";
	}

	@GetMapping("/flags")
	public String guessFlags(Model model) {
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		final String sql = "SELECT flag, amount FROM FlagGuess WHERE guesser = ? AND year = ?";
		List<Map<String, Object>> sqlRes = jdbcTemplate.queryForList(sql, user.get().id, year);
		Flags flags = new Flags();
		for (Map<String, Object> row : sqlRes) {
			String flag = (String) row.get("flag");
			int amount = (int) row.get("amount");
			switch (flag) {
				case "Yellow Flag":
					flags.yellow = amount;
					break;
				case "Red Flag":
					flags.red = amount;
					break;
				case "Safety Car":
					flags.safetyCar = amount;
					break;
			}
		}
		model.addAttribute("flags", flags);
		return "guessFlags";
	}

	@PostMapping("/flags")
	public String guessFlags(@RequestParam int yellow, @RequestParam int red,
			@RequestParam int safetyCar, Model model) {
		Flags flags = new Flags(yellow, red, safetyCar);
		if (!flags.hasValidValues()) {
			model.addAttribute("error", "Verdier kan ikke være negative");
			return "guessFlags";
		}
		Optional<User> user = userService.loadUser();
		if (!user.isPresent()) {
			return "redirect:/";
		}
		final String sql = "REPLACE INTO FlagGuess (guesser, flag, year, amount) values (?, ?, ?, ?)";
		jdbcTemplate.update(sql, user.get().id, "Yellow Flag", year, flags.yellow);
		jdbcTemplate.update(sql, user.get().id, "Red Flag", year, flags.red);
		jdbcTemplate.update(sql, user.get().id, "Safety Car", year, flags.safetyCar);
		logger.info("Guessed '{}' yellow flags, '{}' red flags and '{}' safety cars", flags.yellow, flags.red,
				flags.safetyCar);
		return "redirect:/guess?success=true";
	}

	class PositionedItem implements Comparable<PositionedItem> {
		public final int pos;
		public final String value;

		public PositionedItem(int pos, String value) {
			this.pos = pos;
			this.value = value;
		}

		@Override
		public int compareTo(PositionedItem item) {
			if (pos > item.pos) {
				return 1;
			} else if (pos < item.pos) {
				return -1;
			}
			return 0;
		}
	}

	class Flags {
		public int yellow;
		public int red;
		public int safetyCar;

		public boolean hasValidValues() {
			return yellow >= 0 && red >= 0 && safetyCar >= 0;
		}

		public Flags() {
		}

		public Flags(int yellow, int red, int safetyCar) {
			this.yellow = yellow;
			this.red = red;
			this.safetyCar = safetyCar;
		}
	}
}
