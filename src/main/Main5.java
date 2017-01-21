package main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import elements.AuthenucationTest;
import elements.AuthenucationUI;
import elements.Constants;
import elements.FileUtils;
import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

/**
 * In this version, the capability of keeping track of previous responses was implemented
 * @author Grayson Spidle
 *
 */
public class Main5 {
	
	private static final int FIFTEEN_MINUTES_IN_MILISECONDS = 900000;
	
	public static String BOT_HANDLE = "";
	
	private static final String CONSUMER_KEY = "";
	private static final String CONSUMER_SECRET = "";
	
	private static String accessToken;
	private static String accessTokenSecret;
	
	private static Twitter twitter;
	
	private static ChatterBotFactory factory = new ChatterBotFactory();
	private static ChatterBot bot;
	private static ChatterBotSession session; 
	
	public static boolean post = true;
	
	private static ResponseList<Status> mentions;
	private static ResponseList<Status> tweets;
	
	private static AuthenucationUI authenucationUi;
	
	private static java.util.Date date = null;
	private static long lastID = 0;
	
	public static void main(String[] args) throws Exception {
		
		authenucationUi = new AuthenucationUI(); // Initializes the AuthenucationUI
		
		lastID = FileUtils.getLastRecentReplyID(); // Gets the last tweet id the bot replied to
		
		try { // Attempting to get the access tokens
			accessToken = FileUtils.getKey();
			accessTokenSecret = FileUtils.getSecret();
		} catch (IndexOutOfBoundsException e) { // Upon failure, it will register the user
			AuthenucationTest.register();
		}
		
		initializeTwitter();
		initializeCleverBot();
		BOT_HANDLE = twitter.getScreenName(); // Gets the bot's handle
		
		while (post) { // Main loop
			System.out.println(LocalDateTime.now().getHour() + ":" + LocalDateTime.now().getMinute());
			checkInboxForUnsubscriptions(); // Check for unsubscriptions and blacklist new requests 
			mentions = getMentions(); // Gets Mentions
			tweets = getTweets(); // Gets Tweets
			lastID = FileUtils.getLastRecentReplyID();
			List<Status> queue = createQueue(mentions, tweets); // Create the queue
			for (Status s : queue) { // Goes through every status in the queue
				respond(s); // Responds to them.
				System.out.println("Sleeping thread for 1 minute...");
				System.out.println();
				Thread.sleep(60000); // Delays the next response by 1 minute
			}
			if (queue.size() == 0) {
				System.out.println("Sleeping thread for 1 minute...");
				System.out.println();
				Thread.sleep(60000);
			}
			System.out.println("*****************************");
		}
	}
	
	/**
	 * Initializes the Twitter session and validates the authorization.
	 */
	private static void initializeTwitter() {
		System.out.println("Attempting to initialize Twitter...");
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(CONSUMER_KEY);
		cb.setOAuthConsumerSecret(CONSUMER_SECRET);
		cb.setOAuthAccessToken(FileUtils.getKey());
		cb.setOAuthAccessTokenSecret(FileUtils.getSecret());
	
		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();
		System.out.println("Twitter Initialized.");
	}
	
	/**
	 * Initializes the Cleverbot session.
	 */
	private static void initializeCleverBot() {
		System.out.println("Attempting to initialize CleverBot...");
		try {
			bot = factory.create(ChatterBotType.CLEVERBOT);
			session = bot.createSession();
			System.out.println("CleverBot initialized.");

		} catch (Exception e) {
			System.err.println("Unable to create CleverBot.");
		}
	}
	
	/**
	 * Retrieves the 20 most recent tweets from the bot's timeline.
	 * @return Returns a ResponseList.
	 */
	private static ResponseList<Status> getTweets() {
		ResponseList<Status> tweets = null;
		Vector<Status> tweetsDupe = new Vector<Status>();
		try {
			System.out.println("Attempting to retrieve Tweets...");
			tweets = twitter.getHomeTimeline();
			tweetsDupe.addAll(tweets); // Or else it will throw a concurrentmodification exception
			for (Status s : tweetsDupe) {
				if (s.getUser().getScreenName().equalsIgnoreCase(BOT_HANDLE)) {
					tweets.remove(s);
				}
				else if (s.isRetweet()) {
					tweets.remove(s);
				}
				else if (s.getId() <= lastID) {
					tweets.remove(s);
				}
			}
			System.out.println("Successfully retrieved " + tweets.size() + " Tweets.");
		} catch (TwitterException e) {
			if (e.exceededRateLimitation()) {
				try {
					System.out.println("Unable to retrieve Tweets. Rate Exceeded. Sleeping for 15 mins...");
					Thread.sleep(FIFTEEN_MINUTES_IN_MILISECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		return tweets;
	}
	
	/**
	 * Retrieves the 5 most recent tweets that mention the bot's handle.
	 * @return Returns a ResponseList.
	 */
	private static ResponseList<Status> getMentions() {
		ResponseList<Status> mentions = null;
		Vector<Status> mentionsDupe = new Vector<Status>();
		try {
			System.out.println("Attempting to retrieve Mentions...");
			mentions = twitter.getMentionsTimeline();
			mentionsDupe.addAll(mentions);
			for (Status s : mentionsDupe) {
				if (s.getUser().getScreenName().equalsIgnoreCase(BOT_HANDLE)) {
					mentions.remove(s);
				}
				else if (s.isRetweet()) {
					mentions.remove(s);
				}
				else if (s.getId() <= lastID) {
					mentions.remove(s);
				}
			}
			System.out.println("Successfully retrieved " + mentions.size() +" Mentions.");
		} catch (TwitterException e) {
			if (e.exceededRateLimitation()) {
				try {
					System.out.println("Unable to retrieve Mentions. Rate Exceeded. Sleeping for 15 mins...");
					Thread.sleep(FIFTEEN_MINUTES_IN_MILISECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		return mentions;
	}
	
	/**
	 * Gets a response from the bot, then tweets it.
	 * @param arg0 The status for the bot to reply to.
	 */
	private static void respond(Status arg0) { 
		String message = removeUrls(arg0.getText());
		String name = arg0.getUser().getScreenName();
		try {
			System.out.println("Attempting to respond to @" + name + ": " + message);
			if (!isUserUnsubscribed(arg0.getUser())) {
				String response = session.think(message);
				boolean isPrevious = true;
				boolean isResponseALetterCount = true;
				while (isPrevious) {
					response = session.think(message);
					isPrevious = isAPreviousResponse(response);
				}
				while (isResponseALetterCount) {
					response = session.think(message);
					isResponseALetterCount = isALetterCountResponse(response);
				}
				write(response);
				StatusUpdate update = new StatusUpdate("@" + name + " " + response).inReplyToStatusId(arg0.getId());
				twitter.updateStatus(update);
				System.out.println("Successfully responded to @" + name + ": " + response);
			}
		} catch (Exception e) {
			System.err.println("Unable to respond to @" + name);
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a queue for the bot to respond. Prioritizes mentions over tweets from its feed. The max size for the queue is 15. 
	 * @param mentions The mentions for the bot to add.
	 * @param tweets The tweets for the bot to add if there is space in the queue.
	 * @return Returns a Vector of statuses.
	 */
	private static List<Status> createQueue(ResponseList<Status> mentions, ResponseList<Status> tweets) {
		System.out.println("Creating queue...");
		Vector<Status> output = new Vector<Status>();
		long max = lastID;
		for (Status s : mentions) {
			if (output.size() < 21 && s.getId() >= lastID) {
				output.add(s);
				max = Math.max(max, s.getId());
			}
		}
		for (Status s : tweets) {
			if (output.size() < 21 && s.getId() >= lastID) {
				output.add(s);
				max = Math.max(max, s.getId());
			}
		}
		FileUtils.write(max);
		lastID = FileUtils.getLastRecentReplyID();
		System.out.println("Created queue with a size of " + output.size() + ".");
		return output;
	}
	
	/**
	 * Checks inbox for new unsubscription requests and will add those users to the blacklist.
	 */
	private static void checkInboxForUnsubscriptions() {
		ResponseList<DirectMessage> messages = null;
		try {
			System.out.println("Attempting to retrieve direct messages...");
			messages = twitter.getDirectMessages();
		} catch (TwitterException e) {
			System.err.println("Unable to retrieve direct messages. Exiting...");
			e.printStackTrace();
			System.exit(0);
		}
		int i = 0;
		if (messages != null) {
			System.out.println("Found " + messages.size() + " direct messages...");
			for (DirectMessage dm : messages) {
				String message = dm.getText();
				User sender = dm.getSender();
				message = message.trim();
				if (message.equalsIgnoreCase("unsubscribe")) {
					if (!isUserUnsubscribed(sender)) {
						FileUtils.write(sender.getScreenName());
						i++;
					}
				}
			}
			System.out.println("Unsubscribed " + i + " user(s)." );
		}
		else {
			System.out.println("No direct messages found.");
		}
	}
	
	/**
	 * Checks if the specified user is unsubscribed/blacklisted.
	 * @param user The user to test.
	 * @return Returns true or false indicating whether the user is unsubscribed.
	 */
	private static boolean isUserUnsubscribed(User user) {
		String name = user.getScreenName();
		List<String> blacklisted = FileUtils.getBlacklistedScreenNames();
		for (String s : blacklisted) {
			if (name.contentEquals(s)) {
				return true;
			}
			else if (name.equalsIgnoreCase(BOT_HANDLE)) { 
				return true;
			}
		}
		return false;
	}
	
	public static void exit() {
		System.exit(0);
	}
	
	private static String removeUrls(String input) {
		StringTokenizer tokenizer = new StringTokenizer(input, " ");
		String filteredInput = input;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			try {
				URL temp = new URL(token);
				filteredInput = filteredInput.replaceAll(token, "");
			} catch (MalformedURLException e) {
				// Nothing
			}
		}
		return filteredInput;
	}
	
	private static void write(String s) {
		Calendar cal = advanceAWeek(Calendar.getInstance());
		int day = cal.get(Calendar.DAY_OF_YEAR);
		int year = cal.get(Calendar.YEAR);
		s = s.trim() + "\t" + String.valueOf(day) + ":" + String.valueOf(year) + "\n";
		try {
			Files.write(Constants.PREVIOUS_RESPONSES.toPath(), s.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> getPreviousResponses() {
		try {
			ConcurrentSkipListSet<String> lines = new ConcurrentSkipListSet<String>();
			lines.addAll(Files.readAllLines(Constants.PREVIOUS_RESPONSES.toPath()));
			List<String> output = new Vector<String>();
			for (String s : lines) {
				if (hasResponseExpired(s)) {
					lines.remove(s);
				}
				else {
					StringTokenizer st = new StringTokenizer(s, "\t");
					output.add(st.nextToken());
				}
			}
			System.out.println(lines.size());
			updateResponses(lines);
			return output;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static boolean isAPreviousResponse(String arg0) {
		for (String s : getPreviousResponses()) {
			if (s.equals(arg0)) return true;
		}
		return false;
	}
	
	private static boolean isALetterCountResponse(String arg0) {
		StringTokenizer st = new StringTokenizer(arg0, " ");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			try {
				Integer.parseInt(token);
				if (token.equals("letters")) {
					return true;
				}
			} catch (NumberFormatException e) {
				// Nothing
			}
		}
		return false;
	}
	
	private static boolean hasResponseExpired(String line) {
		try {
			StringTokenizer st = new StringTokenizer(line, "\t");
			st.nextToken();
			String date = st.nextToken();
			st = new StringTokenizer(date, ":");
			int day = Integer.parseInt(st.nextToken());
			int year = Integer.parseInt(st.nextToken());
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DAY_OF_YEAR, day);
			cal.set(Calendar.YEAR, year);
			return cal.before(Calendar.getInstance());
		} catch (NoSuchElementException e) {
			return false;
		}
	}
	
	private static Calendar advanceAWeek(Calendar arg0) {
		Calendar output = Calendar.getInstance();
		int day = arg0.get(Calendar.DAY_OF_YEAR) + 7;
		int year = arg0.get(Calendar.YEAR);
		if (day > 365) {
			day = day - 365;
			year++;
		}
		output.set(Calendar.DAY_OF_YEAR, day);
		output.set(Calendar.YEAR, year);
		return output;
	}
	
	private static void updateResponses(Collection<String> lines) throws IOException {
		Files.write(Constants.PREVIOUS_RESPONSES.toPath(), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		for (String s : lines) {
			s = s + "\n";
			Files.write(Constants.PREVIOUS_RESPONSES.toPath(), s.getBytes(), StandardOpenOption.APPEND);
		}
	}
}
