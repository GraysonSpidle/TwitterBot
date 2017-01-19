package elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import main.Main;

public class FileUtils {

	public static final File BANNED_RESPONSES = new File("bannedResponses.txt");
	public static final File PREVIOUS_RESPONSES = new File("previousResponses.txt");
	public static final File LAST_ID = new File("lastID.txt");
	public static final File BLACKLIST = new File("blacklist.txt");
	public static final File ACCESS_KEYS = new File("accessKeys.txt");

	public static void write(String userScreenName) {
		try {
			Files.write(BLACKLIST.toPath(), userScreenName.getBytes(), StandardOpenOption.APPEND);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void write(String accessKey, String accessKeySecret) {
		try {
			Files.write(ACCESS_KEYS.toPath(), accessKey.getBytes());
			Files.write(ACCESS_KEYS.toPath(), accessKeySecret.getBytes(), StandardOpenOption.APPEND);
		} catch (FileNotFoundException e1) {
			System.err.println("Unable to find the the file.");
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void addBannedResponse(String response) {
		StringTokenizer st = new StringTokenizer(response, " ");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("@")) {
				response = response.replace(token, "");
			}
		}
		try {
			Files.write(BANNED_RESPONSES.toPath(), response.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println("Unable to update the banned response list. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Writes the last status id into the lastID.txt file
	 * 
	 * @param id
	 */
	public static void write(long id) {
		try {
			if (id > FileUtils.getLastRecentReplyID()) {
				Files.write(LAST_ID.toPath(), String.valueOf(id).getBytes());
				System.out.println("Successfully updated the previous id to " + id + ". ");
			}
		} catch (FileNotFoundException e) {
			System.err.println("Unable to find the the file. Exiting...");
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Unable to write the previous id. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void addResponse(String response) {
		try {
			StringTokenizer st = new StringTokenizer(response, " ");
			String modifiedResponse = response;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.contains("@"))
					modifiedResponse = modifiedResponse.replace(token, "");
				try {
					URL temp = new URL(token);
					modifiedResponse = modifiedResponse.replace(token, "");
				} catch (MalformedURLException e) {
					// Nothing
				}
			}
			Files.write(PREVIOUS_RESPONSES.toPath(), (getToday() + " " + String.valueOf(modifiedResponse)).getBytes(),
					StandardOpenOption.APPEND);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to find the the file. Exiting...");
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Unable to write the previous response. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static long getLastRecentReplyID() {
		try {
			List<String> output = Files.readAllLines(LAST_ID.toPath(), Charset.defaultCharset());
			return Long.parseLong(output.get(0));
		} catch (IOException e) {
			System.err.println("Failed to get the last status id.");
			e.printStackTrace();
			System.exit(0);
		}
		return 0;
	}

	public static List<String> getBlacklistedScreenNames() {
		try {
			List<String> output = Files.readAllLines(BLACKLIST.toPath(), Charset.defaultCharset());
			output.add(Main.BOT_HANDLE);
			return output;
		} catch (IOException e) {
			return null;
		}

	}

	public static String getKey() throws ArrayIndexOutOfBoundsException {
		try {
			List<String> lines = Files.readAllLines(ACCESS_KEYS.toPath(), Charset.defaultCharset());
			return lines.get(0);
		} catch (IOException e) {
			System.err.println("Unable to find the the file.");
			e.printStackTrace();
		}
		return null;
	}

	public static String getSecret() {
		try {
			List<String> lines = Files.readAllLines(ACCESS_KEYS.toPath(), Charset.defaultCharset());
			return lines.get(1);
		} catch (IOException e) {
			System.err.println("Unable to find the the file.");
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getBannedResponses() {
		List<String> output = null;
		try {
			output = Files.readAllLines(BANNED_RESPONSES.toPath());
		} catch (IOException e) {
			System.err.println("Unable to get banned responses. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
		return output;
	}

	public static List<String> getPreviousResponses() {
		List<String> output = null;
		try {
			output = Files.readAllLines(PREVIOUS_RESPONSES.toPath());
		} catch (IOException e) {
			System.err.println("Unable to get previous responses. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
		List<String> output2 = new Vector<String>();
		for (String s : output) {
			StringTokenizer st = new StringTokenizer(s, " ");
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < st.countTokens(); i++) {
				if (i > 0) b.append(st.nextToken() + " ");
			}
			output2.add(b.toString());
		}
		return output;
	}
	
	public static List<String> getPreviousResponsesWithDates() {
		List<String> output = null;
		try {
			output = Files.readAllLines(PREVIOUS_RESPONSES.toPath());
		} catch (IOException e) {
			System.err.println("Unable to get previous responses. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
		return output;
	}
	
	public static void deleteOldResponses() {
		List<String> responses = getPreviousResponsesWithDates();
		List<String> newLines = new Vector<String>();
		for (String s : responses) {
			StringTokenizer st = new StringTokenizer(s, " ");
			DateFormat f = new SimpleDateFormat("dd/MM/yy");
			try {
				Date date = f.parse(st.nextToken());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
		}
	}

	private static String getToday() {
		DateFormat df = new SimpleDateFormat("dd/MM/yy");
		Calendar cal = Calendar.getInstance();
		return df.format(cal.getTime());
	}
	
	private static int getDay(String date) {
		StringTokenizer st = new StringTokenizer(date, "/");
		return Integer.parseInt(st.nextToken());
	}
	
	private static int getMonth(String date) {
		StringTokenizer st = new StringTokenizer(date, "/");
		st.nextToken();
		return Integer.parseInt(st.nextToken());
	}
	
	private static int getYear(String date) {
		StringTokenizer st = new StringTokenizer(date, "/");
		st.nextToken();
		st.nextToken();
		return Integer.parseInt(st.nextToken());
	}

}
