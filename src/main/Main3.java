package main;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import elements.AuthenucationTest;
import elements.AuthenucationUI;
import elements.FileUtils;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class Main3 {
	
	private static final int CODE_DUPLICATE = 187;
	private static final int CODE_MUZZLED = 261;
	private static final int CODE_VERIFY = 231;
	
	private static final int FIFTEEN_MINUTES_IN_MILISECONDS = 900000;
	
	private static final int MAX_MENTIONS_IN_STATUS = 4;
	
	public static String BOT_HANDLE = "";
	
	private static final String CONSUMER_KEY = "TJxsh4CCCAnH0gs5nusxNGTVE";
	private static final String CONSUMER_SECRET = "bZIFmy1jToFv9IvUxQKcIu0vNMSqMT2TcqSGDgHwbpyNoEtujZ";
	
	private static String accessToken = "";
	private static String accessTokenSecret = "";
	
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
	
	public static void main(String[] args) throws InterruptedException {
		
		authenucationUi = new AuthenucationUI(); // Initializes the AuthenucationUI
		
		lastID = FileUtils.getLastRecentReplyID(); // Gets the last tweet id the bot replied to
		
		try {
			try { // Attempting to get the access tokens
				accessToken = FileUtils.getKey();
				accessTokenSecret = FileUtils.getSecret();
			} catch (IndexOutOfBoundsException e) { // Upon failure, it will register the user
				AuthenucationTest.register();
			}
		} catch (TwitterException e) {
			System.err.println("Failed to authenucate.");
			e.printStackTrace();
			System.exit(0);
		} catch (Exception e) {
			System.err.println("Failed to authenucate.");
			e.printStackTrace();
			System.exit(0);
		}
		
		initializeTwitter();
		initializeCleverBot();
		try {
			BOT_HANDLE = twitter.getScreenName(); // Gets the bot's handle
		} catch (TwitterException e) {
			if (e.getErrorCode() == CODE_VERIFY) {
				System.err.println("Failed to get handle due to a lack of authenucation.");
				e.printStackTrace();
				System.exit(0);
			}
			else {
				System.err.println("Failed to get handle.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		while (post) { // Main loop
			try {
				checkInboxForUnsubscriptions(); // Check for unsubscriptions and blacklist new requests 
				mentions = getMentions(); // Get Mentions
				tweets = getTweets(); // Get Tweets
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
			} catch (TwitterException e) {
				if (e.exceededRateLimitation()) {
					RateLimitStatus limit = e.getRateLimitStatus();
					System.err.println(e.getMessage());
					System.out.println("Sleeping thread for " + String.valueOf((limit.getSecondsUntilReset() + 1)) + " seconds.");
					Thread.sleep((limit.getSecondsUntilReset() * 1000) + 1);
				}
				else if (e.isCausedByNetworkIssue()) {
					System.err.println("Encountered a network issue.");
					System.out.println("Sleeping thread for 10 minutes.");
					Thread.sleep(10000);
				}
				else if (e.resourceNotFound()) {
					System.err.println("A resource was not found. Exiting...");
					e.printStackTrace();
					System.exit(0);
				}
				else if (e.getErrorCode() == CODE_MUZZLED) {
					System.err.println("The application was muzzled, and cannot perform write actions. Exiting...");
					System.err.println("Go to https://support.twitter.com/forms/platform to appeal the muzzle.");
					e.printStackTrace();
					System.exit(0);
				}
				else {
					System.err.println("Encountered an unchecked error. Exiting...");
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		
	}
	
	/**
	 * Initializes the Twitter session and validates the authorization.
	 */
	private static void initializeTwitter() {
		try {
			System.out.println("Attempting to initialize Twitter...");
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setOAuthConsumerKey(CONSUMER_KEY);
			cb.setOAuthConsumerSecret(CONSUMER_SECRET);
			cb.setOAuthAccessToken(FileUtils.getKey());
			cb.setOAuthAccessTokenSecret(FileUtils.getSecret());
			TwitterFactory tf = new TwitterFactory(cb.build());
			twitter = tf.getInstance();
			System.out.println("Twitter was initialized.");
		} catch (Exception e) {
			System.err.println("Failed to initialize Twitter.");
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	/**
	 * Initializes the Cleverbot session.
	 */
	private static void initializeCleverBot() {
		try {
			System.out.println("Attempting to initialize CleverBot...");
			bot = factory.create(ChatterBotType.CLEVERBOT);
			session = bot.createSession();
			System.out.println("CleverBot was initialized.");
		} catch (Exception e) {
			System.err.println("Failed to initialize CleverBot.");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * Retrieves the 20 most recent tweets from the bot's timeline.
	 * @return Returns a ResponseList.
	 */
	private static ResponseList<Status> getTweets() throws TwitterException {
		ResponseList<Status> tweets = null;
		Vector<Status> tweetsDupe = new Vector<Status>();
		Paging paging = new Paging(lastID);
		System.out.println("Attempting to retrieve Tweets...");
		tweets = twitter.getHomeTimeline(paging);
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
		return tweets;
	}
	
	/**
	 * Retrieves the 20 most recent tweets that mention the bot's handle.
	 * @return Returns a ResponseList.
	 */
	private static ResponseList<Status> getMentions() throws TwitterException {
		ResponseList<Status> mentions = null;
		Vector<Status> mentionsDupe = new Vector<Status>();
		Paging paging = new Paging(lastID);
		System.out.println("Attempting to retrieve Mentions...");
		mentions = twitter.getMentionsTimeline(paging);
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
		return mentions;
	}
	
	/**
	 * Gets a response from the bot, then tweets it.
	 * @param arg0 The status for the bot to reply to.
	 * @throws TwitterException 
	 */
	private static void respond(Status arg0) throws TwitterException { 
		String message = arg0.getText();
		String name = arg0.getUser().getScreenName();
		String response = null;
		try {
			System.out.println("Attempting to respond to @" + name + ": " + message);
			if (!isUserUnsubscribed(arg0.getUser())) {
				response = "@" + name + " " + session.think(removeUrls(message));
				if (!isBannedResponse(response) && !isPreviousResponse(response)) { 
					StatusUpdate update = new StatusUpdate(response).inReplyToStatusId(arg0.getId());
					twitter.updateStatus(update);
					FileUtils.addResponse();
					System.out.println("Successfully responded to @" + name + ": " + response);
				}
				else {
					System.out.println("Failed to respond due to selected response being banned: " + response);
				}
			}
		} catch (TwitterException e) {
			System.err.println("Unable to respond to @" + name + "with: " + response);
			if (e.getErrorCode() == CODE_DUPLICATE) {
				FileUtils.addBannedResponse(response);
			}
			else {
				throw e;
			}
		} catch (Exception e) {
			System.err.println("Unable to respond to @" + name + "with: " + response);
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
	
	private static boolean containsUrls(String input) {
		StringTokenizer tokenizer = new StringTokenizer(input, " ");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			try {
				URL temp = new URL(token);
				return true;
			} catch (MalformedURLException e) {
				// Nothing
			}
		}
		return false;
	}
	
	private static boolean containsTooManyMentions(String input) {
		StringTokenizer st = new StringTokenizer(input, " ");
		int counter = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("@")) counter++;
			if (counter > MAX_MENTIONS_IN_STATUS) return true;
		}
		return false;
	}
	
	private static boolean isBannedResponse(String response) {
		List<String> bannedResponses = FileUtils.getBannedResponses();
		for (String s : bannedResponses) {
			if (s.equals(response)) return true;
		}
		if (containsUrls(response)) return true;
		if (containsTooManyMentions(response)) return true;
		if (response.contains("#")) return true;
		if (response.length() > 140) return true;
		return false;
	}
	
	private static boolean isPreviousResponse(String response) {
		List<String> previousResponses = FileUtils.getPreviousResponses();
		for (String s : previousResponses) {
			if (s.equals(response)) return true;
		}
		return false;
	}
	
}
