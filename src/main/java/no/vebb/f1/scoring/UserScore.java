package no.vebb.f1.scoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;

import no.vebb.f1.database.Database;
import no.vebb.f1.util.Table;

public class UserScore {

	private Database db;

	private final UUID id;
	private final int year;
	private int score;
	private final int raceNumber;
	private final int racePos;
	public final Table driversTable;
	public final Table constructorsTable;
	public final Table flagsTable;
	public final Table winnerTable;
	public final Table tenthTable;
	public final Table summaryTable;
	private final List<List<String>> summaryTableBody = new ArrayList<>();

	public UserScore(UUID id, int year, int raceNumber, Database db) {
		this.id = id;
		this.year = year;
		this.raceNumber = raceNumber;
		this.db = db;
		this.racePos = getRacePosition();
		this.driversTable = initializeDriversTable();
		this.constructorsTable = initializeConstructorsTable();
		this.flagsTable = initializeFlagsTable();
		this.winnerTable = initializeWinnerTable();
		this.tenthTable = initializeTenthTable();
		this.summaryTable = initializeSummaryTable();
	}

	public UserScore(UUID id, int year, Database db) {
		this(id, year, UserScore.getRaceNumber(year, db), db);
	}

	private static int getRaceNumber(int year, Database db) {
		try {
			return db.getLatestRaceNumber(year);
		} catch (EmptyResultDataAccessException e) {
			return -1;
		}
	}

	private int getRacePosition() {
		if (raceNumber == -1) {
			return 0;
		}
		return db.getPositionOfRace(raceNumber);
	}

	private Table initializeDriversTable() {
		DiffPointsMap map = new DiffPointsMap("DRIVER", year, db);
		List<String> header = Arrays.asList("Plass", "Sjåfør", "Gjettet", "Diff", "Poeng");

		List<String> guessedDriver = db.getGuessedYearDriver(year, id);
		List<String> drivers = db.getDriverStandings(raceNumber, year);
		return getGuessedToPos(map, "driver", header, guessedDriver, drivers);
	}

	private Table initializeConstructorsTable() {
		DiffPointsMap map = new DiffPointsMap("CONSTRUCTOR", year, db);
		List<String> header = Arrays.asList("Plass", "Konstruktør", "Gjettet", "Diff", "Poeng");

		List<String> guessedConstructor = db.getGuessedYearConstructor(year, id);
		List<String> constructors = db.getConstructorStandings(raceNumber, year);

		return getGuessedToPos(map, "constructor", header, guessedConstructor, constructors);
	}

	private Table getGuessedToPos(DiffPointsMap map, String category, List<String> header, List<String> guessed, List<String> competitors) {
		List<List<String>> body = new ArrayList<>();
		int competitorScore = 0;
		Map<String, Integer> guessedToPos = new HashMap<>();
		for (int i = 0; i < guessed.size(); i++) {
			guessedToPos.put(guessed.get(i), i+1);
		}
		for (int i = 0; i < competitors.size(); i++) {
			List<String> row = new ArrayList<>();
			int actualPos = i + 1;
			row.add(String.valueOf(actualPos));
			String driver = competitors.get(i);
			row.add(driver);
			Integer pos = guessedToPos.get(driver);
			if (pos == null) {
				row.add("N/A");
				row.add("N/A");
				row.add("0");
			} else {
				int diff = Math.abs(actualPos - pos); 
				int points = map.getPoints(diff);
				competitorScore += points;
				row.add(pos.toString());
				row.add(String.valueOf(diff));
				row.add(String.valueOf(points));
				
			}
			body.add(row);
		}
		score += competitorScore;
		String translation = db.translateCategory(category.toUpperCase());
		summaryTableBody.add(Arrays.asList(translation, String.valueOf(competitorScore)));
		return new Table(translation, header, body);
	}

	private Table initializeFlagsTable() {
		String category = "FLAG";
		DiffPointsMap map = new DiffPointsMap(category, year, db);
		List<String> header = Arrays.asList("Type", "Gjettet", "Faktisk", "Diff", "Poeng");
		List<List<String>> body = new ArrayList<>();
		int flagScore = 0;

		List<Map<String, Object>> sqlRes = db.getDataForFlagTable(racePos, year, id);

		for (Map<String, Object> row : sqlRes) {
			String flag = db.translateFlagName((String) row.get("type"));
			int guessed = (int) row.get("guessed");
			int actual = (int) row.get("actual");
			int diff = Math.abs(guessed - actual);
			int points = map.getPoints(diff);
			flagScore += points;
			body.add(Arrays.asList(flag, String.valueOf(guessed), String.valueOf(actual), String.valueOf(diff), String.valueOf(points))); 
		}

		Collections.sort(body, (a, b) -> a.get(0).compareTo(b.get(0)));

		score += flagScore;
		String translation = db.translateCategory(category);
		summaryTableBody.add(Arrays.asList(translation, String.valueOf(flagScore)));
		return new Table(translation, header, body);
	}
	
	private Table initializeWinnerTable() {
		return getDriverPlaceGuessTable("FIRST", 1);
	}
	
	private Table initializeTenthTable() {
		return getDriverPlaceGuessTable("TENTH", 10);
	}

	private Table getDriverPlaceGuessTable(String category, int targetPos) {
		List<String> header = Arrays.asList("Løp", "Gjettet", "Startet", "Plass", "Poeng");
		List<List<String>> body = new ArrayList<>();
		String translation = db.translateCategory(category);
		if (raceNumber == -1) {
			return new Table(translation, header, body);
		}
		DiffPointsMap map = new DiffPointsMap(category, year, db);
		int driverPlaceScore = 0;
		List<Map<String, Object>> sqlRes = db.getDataForPlaceGuessTable(category, id, year, racePos);

		for (Map<String, Object> row : sqlRes) {
			String raceName = (String) row.get("race_name");
			String driver = (String) row.get("driver");
			int startPos = (int) row.get("start");
			int finishPos = (int) row.get("finish");
			int diff = Math.abs(targetPos - finishPos);
			int points = map.getPoints(diff);
			driverPlaceScore += points;
			body.add(Arrays.asList(raceName, driver, String.valueOf(startPos), String.valueOf(finishPos), String.valueOf(points)));
		}
		score += driverPlaceScore;

		summaryTableBody.add(Arrays.asList(translation, String.valueOf(driverPlaceScore)));
		return new Table(translation, header, body);
	}
	
	private Table initializeSummaryTable() {
		List<String> header = Arrays.asList("Kategori", "Poeng");
		return new Table("Oppsummering", header, summaryTableBody);
	}

	public int getScore() {
		return score;
	}

	public List<Table> getAllTables() {
		List<Table> tables = Arrays.asList(summaryTable, driversTable, constructorsTable, flagsTable, winnerTable, tenthTable);
		return tables;
	}
}
