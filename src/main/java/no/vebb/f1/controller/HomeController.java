package no.vebb.f1.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import no.vebb.f1.scoring.UserScore;
import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;
import no.vebb.f1.util.Cutoff;
import no.vebb.f1.util.Table;

import org.springframework.ui.Model;

@Controller
public class HomeController {

	@Autowired
	private UserService userService;

	@Autowired
	private Cutoff cutoff;
	
	private JdbcTemplate jdbcTemplate;
	private int year = 2025;

	public HomeController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/")
	public String home(Model model) {
		boolean loggedOut = !userService.isLoggedIn();
		model.addAttribute("loggedOut", loggedOut);
		Table leaderBoard = getLeaderBoard();
		model.addAttribute("leaderBoard", leaderBoard);
		model.addAttribute("raceGuess", isRaceGuess());

		model.addAttribute("isAdmin", userService.isAdmin());
		return "public";
	}

	private boolean isRaceGuess() {
		final String getRaceIdSql = """
			SELECT ro.id AS id
			FROM RaceOrder ro
			JOIN StartingGrid sg ON ro.id = sg.race_number
			WHERE ro.year = ?
			ORDER BY ro.position DESC
			LIMIT 1;
		""";
		int year = cutoff.getCurrentYear();
		try {
			int raceId = jdbcTemplate.queryForObject(getRaceIdSql, Integer.class, year);
			return !cutoff.isAbleToGuessRace(raceId);
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

	private Table getLeaderBoard() {
		List<String> header = Arrays.asList("Plass", "Navn", "Poeng");
		List<List<String>> body = new ArrayList<>();
		List<Guesser> leaderBoardUnsorted = new ArrayList<>();
		if (cutoff.isAbleToGuessCurrentYear()) {
			return new Table("Sesongen starter snart", new ArrayList<>(), new ArrayList<>());
		}
		final String getAllUsersSql = "SELECT id FROM User";
		List<UUID> userIds = jdbcTemplate.query(getAllUsersSql, (rs, rowNum) -> UUID.fromString(rs.getString("id")));
		for (UUID id : userIds) {
			User user = userService.loadUser(id).get();
			UserScore userScore = new UserScore(user, year, jdbcTemplate);
			leaderBoardUnsorted.add(new Guesser(user.username, userScore.getScore(), user.id));
		}

		leaderBoardUnsorted.removeIf(guesser -> guesser.points == 0);
		Collections.sort(leaderBoardUnsorted);
		
		for (int i = 0; i < leaderBoardUnsorted.size(); i++) {
			Guesser guesser = leaderBoardUnsorted.get(i);
			body.add(Arrays.asList(String.valueOf(i+1), guesser.username, String.valueOf(guesser.points), guesser.id.toString()));
		}
		return new Table("Rangering", header, body);
	}

	class Guesser implements Comparable<Guesser> {

		public final String username;
		public final int points;
		public final UUID id;

		public Guesser(String username, int points, UUID id) {
			this.username = username;
			this.points = points;
			this.id = id;
		}

		@Override
		public int compareTo(Guesser other) {
			if (points < other.points) {
				return 1;
			} else if (points > other.points) {
				return -1;
			}
			return 0;
		}
	}
}
