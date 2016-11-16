package main;

import java.sql.Time;
import java.sql.Date;
import java.util.List;
import java.util.Vector;

import cleverbot.ChatterBot;
import cleverbot.ChatterBotFactory;
import cleverbot.ChatterBotSession;
import cleverbot.ChatterBotType;
import elements.AuthenucationTest;
import elements.BlacklistException;
import elements.FileUtils;
import elements.UI;
import twitter4j.DirectMessage;
import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class Main {
	
	private static final int FIFTEEN_MINUTES_IN_MILISECONDS = 900000;
	
	public static String BOT_HANDLE = "";
	
	private static final String CONSUMER_KEY = "nE73hD0zYynB3ys7jKocbOA2C";
	private static final String CONSUMER_SECRET = "vQvzq9anBNDA5RXDqbmLrA5gC83R1RlsXZwauYXMbcjBRzztlH";
	
	private static String accessToken = "";
	private static String accessTokenSecret = "";
	
	static Twitter twitter;
	
	static ChatterBotFactory factory = new ChatterBotFactory();
	static ChatterBot bot;
	static ChatterBotSession session; 
	public static boolean post = true;
	
	private static ResponseList<Status> mentions;
	private static ResponseList<Status> tweets;
	
	private static UI ui;
	
	static java.util.Date date = null;
	static long lastID = 0;
	
	public static void main(String[] args) throws Exception {
		
		ui = new UI(); // Initializes the UI
		
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
			checkInboxForUnsubscriptions(); // Check for unsubscriptions and blacklist new requests 
			mentions = getMentions(); // Gets Mentions
			tweets = getTweets(); // Gets Tweets
			Vector<Status> queue = createQueue(mentions, tweets); // Create the queue
			for (Status s : queue) { // Goes through every status in the queue
				respond(s); // Responds to them.
				System.out.println("Sleeping thread for 1 minute...");
				System.out.println();
				Thread.sleep(60000); // Delays the next response by 1 minute
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
			System.out.println("Successfully retrieved Tweets.");
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
	 * Retrieves the 20 most recent tweets that mention the bot's handle.
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
			System.out.println("Successfully retrieved Mentions.");
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
		String message = arg0.getText();
		String name = arg0.getUser().getScreenName();
		try {
			System.out.println("Attempting to respond to @" + name + ": " + message);
			if (!isUserUnsubscribed(arg0.getUser())) {
				String response = "@" + name + " " + session.think(message);
				StatusUpdate update = new StatusUpdate(response).inReplyToStatusId(arg0.getId());
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
	private static Vector<Status> createQueue(ResponseList<Status> mentions, ResponseList<Status> tweets) {
		System.out.println("Creating queue...");
		Vector<Status> output = new Vector<Status>();
		for (Status s : mentions) {
			if (output.size() < 15) {
				output.add(s);
			}
			if (s.getId() > lastID) {
				FileUtils.write(s.getId());
			}
		}
		for (Status s : tweets) {
			if (output.size() < 15) {
				output.add(s);
			}
			if (s.getId() > lastID) {
				FileUtils.write(s.getId());
			}
		}
		System.out.println("Created queue with a size of " + output.size() + ".");
		try {
			FileUtils.write(output.get(0).getId());
		} catch (ArrayIndexOutOfBoundsException e) {
			lastID = FileUtils.getLastRecentReplyID();
		}
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
}
