package no.vebb.f1.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.vebb.f1.database.Database;
import no.vebb.f1.user.User;
import no.vebb.f1.user.UserService;
import no.vebb.f1.util.domainPrimitive.RaceId;

@Component
public class BreadcrumbInterceptor implements HandlerInterceptor {

	@Autowired
	private Database db;

	@Autowired
	private UserService userService;

	@SuppressWarnings("null")
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Map<String, String> breadcrumbs = new LinkedHashMap<>();
		request.setAttribute("breadcrumbs", breadcrumbs);
		String path = request.getRequestURI();
		if (path.equals("/")) {
			breadcrumbs.put("Hjem", null);
			return true;
		}
		addBreadcrumbs(breadcrumbs, path);
		return true;
	}

	private void addBreadcrumbs(Map<String, String> breadcrumbs, String path) {
		breadcrumbs.put("Hjem", "/");
		List<String> subPaths = getSubPaths(path);
		subPaths.remove(subPaths.size() - 1);
		for (String subPath : subPaths) {
			breadcrumbs.put(getNameForPath(subPath), subPath);
		}
		breadcrumbs.put(getNameForPath(path), null);
	}

	private List<String> getSubPaths(String path) {
		List<String> result = new ArrayList<>();
		String[] segments = path.split("/");

		StringBuilder currentPath = new StringBuilder();
		for (String segment : segments) {
			if (segment.isEmpty()) {
				continue;
			}
			currentPath.append("/").append(segment);
			result.add(currentPath.toString());
		}

		return result;
	}

	private String getNameForPath(String path) {
		Iterator<String> segments = Arrays.asList(path.split("/")).iterator();
		segments.next(); // First is always blank
		switch (segments.next()) {
			case "admin":
				return getAdminPath(segments);
			case "user":
				return getUserPath(segments);
			case "race-guess":
				return "Tippet på løp";
			case "score":
				return "Poengberegning";
			case "contact":
				return "Kontakt";
			case "about":
				return "Om siden";
			case "guess":
				return getGuessPath(segments);
			case "error":
				return "Feil";
			case "settings":
				return getSettingsPath(segments);
			case "username":
				return "Velg brukernavn";
			case "privacy":
				return "Personvernerklæring";
			case "stats":
				return getStatsPath(segments);
			case "bingo":
				return getBingoPath(segments);
			default:
				return "no path";
		}
	}

	private String getAdminPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Admin portal";
		}
		String category = segments.next();
		switch (category) {
			case "flag":
				return getFlagPath(segments);
			case "season":
				return getSeasonPath(segments);
			case "bingo":
				return "Bingo";
			case "log":
				return getLogPath(segments);
			case "backup":
				return "Sikkerhetskopi";
		}
		return "no path";
	}

	private String getFlagPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Flagg";
		}
		String year = segments.next();
		if (year.matches("add|delete")) {
			return "";
		}
		if (!segments.hasNext()) {
			try {
				return String.valueOf(Integer.parseInt(year));
			} catch (NumberFormatException e) {
				return "no path";
			}
		}
		String id = segments.next();
		if (!segments.hasNext()) {
			RaceId raceId = new RaceId(Integer.parseInt(id), db);
			int position = db.getPositionOfRace(raceId);
			String raceName = db.getRaceName(raceId);
			return position + ". " + raceName;
		}
		return "no path";
	}

	private String getLogPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Logg";
		}
		String type = segments.next();
		if (!segments.hasNext()) {
			if (type.length() > 1) {
				return type.substring(0, 1).toUpperCase() + type.substring(1);
			}
			return type;
		}
		String file = segments.next();
		if (!segments.hasNext()) {
			if (file.length() > 4) {
				return file.substring(0, file.length()-4);
			}
			return file;
		}
		return "no path";
	}

	private String getSeasonPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Sesong";
		}
		String year = segments.next();
		if (!segments.hasNext()) {
			if (year.equals("add")) {
				return "Legg til";
			}
			return year;
		}
		String category = segments.next();
		switch (category) {
			case "competitors":
				return getCompetitorsPath(segments);
			case "points":
				return getPointsPath(segments);
			case "cutoff":
				return getCutoffPath(segments);
			case "manage":
				return getManagePath(segments);
		}
		return "no path";
	}

	private String getCompetitorsPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Deltakere";
		}
		switch (segments.next()) {
			case "constructors":
				return "Konstruktører";
			case "drivers":
				return "Sjåfører";
			case "alias":
				return "Alternative navn";
		}
		return "no path";
	}
	
	private String getPointsPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Poengsystem";
		}
		return "no path";
	}

	private String getCutoffPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Frister";
		}
		return "no path";
	}

	private String getManagePath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Administrer";
		}
		String id = segments.next();
		if (id.matches("reload|move|delete|add")) {
			return "";
		}
		if (!segments.hasNext()) {
			RaceId raceId = new RaceId(Integer.parseInt(id), db);
			int position = db.getPositionOfRace(raceId);
			String raceName = db.getRaceName(raceId);
			return position + ". " + raceName;
		}
		return "no path";
	}

	private String getUserPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Brukere";
		}
		String userProfile = segments.next();
		if (userProfile.equals("myprofile")) {
			return "Min profil";
		}
		if (userProfile.equals("compare")) {
			return "Sammenlign";
		}
		try {
			Optional<User> optUser = userService.loadUser(UUID.fromString(userProfile));
			if (optUser.isEmpty()) {
				return "Bruker ikke funnet";
			}
			return optUser.get().username;
		} catch (IllegalArgumentException e) {
			return "no path";
		}
	}

	private String getGuessPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Tipping";
		}
		String category = segments.next();
		switch (category) {
			case "drivers":
				return "Sjåførmesterskap";
			case "constructors":
				return "Konstruktørmesterskap";
			case "tenth":
				return "10.plass";
			case "winner":
				return "1.plass";
			case "flags":
				return "Antall";
		}
		return "no path";
	}

	private String getSettingsPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Innstillinger";
		}
		String setting = segments.next();
		switch (setting) {
			case "info":
				return "Brukerinformasjon";
			case "username":
				return "Endre brukernavn";
			case "delete":
				return "Slett bruker";
			case "mail":
				return "Påminnelser";
			case "referral":
				return "Inviter brukere";
		}
		return "no path";
	}

	private String getStatsPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Statistikk";
		}

		String year = segments.next();
		if (!segments.hasNext()) {
			return year;
		}
		String id = segments.next();
		if (!segments.hasNext()) {
			RaceId raceId = new RaceId(Integer.parseInt(id), db);
			int position = db.getPositionOfRace(raceId);
			String raceName = db.getRaceName(raceId);
			return position + ". " + raceName;
		}
		return "no path";
	}
	
	private String getBingoPath(Iterator<String> segments) {
		if (!segments.hasNext()) {
			return "Bingo";
		}
		String admin = segments.next();
		if (!segments.hasNext()) {
			if (admin.equals("admin")) {
				return "Administrer bingo";
			}
		}
		String year = segments.next();
		if (!segments.hasNext()) {
			return year;
		}
		return "no path";
	}
}
