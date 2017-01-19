package concurrent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

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

public class Toolbox {
	
	public static StatusUpdate getResponseFromCleverbot(Status inReplyTo) {
		StatusUpdate output = null;
		try {
			ChatterBotSession session = new ChatterBotFactory().create(ChatterBotType.CLEVERBOT).createSession();
			String text = inReplyTo.getText();
			
			// Getting those URLs out of the status
			StringTokenizer tokenizer = new StringTokenizer(text, " ");
			String filteredText = text;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				try {
					URL temp = new URL(token);
					filteredText = filteredText.replaceAll(token, "");
				} catch (MalformedURLException e) {
					// Nothing
				}
			}
			// Getting the response from Cleverbot
			String response = session.think(filteredText);
			output = new StatusUpdate(response).inReplyToStatusId(inReplyTo.getId());
		} catch (Exception e) {
			System.err.println("Unable to initialize CleverBot.");
			return null;
		}
		return output;
	}

	public static ResponseList<Status> getTweetsFromHomeTimeline(Twitter twitter) {
		try {
			final String BOT_HANDLE = twitter.getScreenName(); // Gets the bot's handle
			Paging paging = new Paging(FileUtils.getLastRecentReplyID());
			ResponseList<Status> output = twitter.getHomeTimeline(paging);
			Iterator<Status> iterator = output.iterator();
			while (iterator.hasNext()) {
				Status s = iterator.next();
				if (s.isRetweet()) {
					output.remove(s);
				}
				if (s.getUser().getScreenName().equalsIgnoreCase(BOT_HANDLE)) {
					output.remove(s);
				}
				if (s.getId() <= FileUtils.getLastRecentReplyID()) {
					output.remove(s);
				}
			}
			return output;
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Twitter getTwitter(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
		Twitter output = null;
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(consumerKey);
		cb.setOAuthConsumerSecret(consumerSecret);
		cb.setOAuthAccessToken(accessToken);
		cb.setOAuthAccessTokenSecret(accessTokenSecret);
	
		TwitterFactory tf = new TwitterFactory(cb.build());
		output = tf.getInstance();
		return output;
	}
	
	public static Map<String, RateLimitStatus> getRelevantLimits(Twitter twitter) {
		Map<String, RateLimitStatus> output = new HashMap<String, RateLimitStatus>();
		Map<String, RateLimitStatus> limits = null;
		try {
			limits = twitter.getRateLimitStatus();
		} catch (TwitterException e1) {
			e1.printStackTrace();
		}
		String[] limitNames = {"/statuses/home_timeline","/statuses/mentions_timeline","/direct_messages/show"};
		for (Entry<String, RateLimitStatus> e : limits.entrySet()) {
			boolean match = false;
			for (String s : limitNames) {
				if (e.getKey().equals(s)) {
					match = true;
					break;
				}
			}
			if (match) output.put(e.getKey(), e.getValue());  
		}
		return output;
	}
	
	public static ResponseList<Status> getTweetsFromMentionsTimeline(Twitter twitter) {
		try {
			final String BOT_HANDLE = twitter.getScreenName(); // Gets the bot's handle
			Paging paging = new Paging(FileUtils.getLastRecentReplyID());
			ResponseList<Status> output = twitter.getMentionsTimeline(paging);
			Iterator<Status> iterator = output.iterator();
			while (iterator.hasNext()) {
				Status s = iterator.next();
				if (s.isRetweet()) {
					output.remove(s);
				}
				if (s.getUser().getScreenName().equalsIgnoreCase(BOT_HANDLE)) {
					output.remove(s);
				}
				if (s.getId() <= FileUtils.getLastRecentReplyID()) {
					output.remove(s);
				}
			}
			return output;
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static RateLimitStatus getLimit(String name, Twitter twitter) {
		RateLimitStatus output = null;
		try {
			Map<String, RateLimitStatus> map =  twitter.getRateLimitStatus(name);
			output = map.get(name);
		} catch (TwitterException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return output;
	}
	
	public static synchronized void updateLastId(long id) {
		if (FileUtils.getLastRecentReplyID() < id) {
			FileUtils.write(id);
		}
	}
	
	/**
	 * Checks inbox for new unsubscription requests and will add those users to the blacklist.
	 * @return 
	 */
	public static ResponseList<DirectMessage> getUnsubscriptionsFromInbox(Twitter twitter) {
		try {
			final String BOT_HANDLE = twitter.getScreenName();
			ResponseList<DirectMessage> messages = twitter.getDirectMessages();
			Iterator<DirectMessage> iterator = messages.iterator();
			while (iterator.hasNext()) {
				DirectMessage dm = iterator.next();
				String text = dm.getText().trim();
				User sender = dm.getSender();
				if (text.equalsIgnoreCase("unsubscribe")) {
					if (!isUserUnsubscribed(sender)) {
						FileUtils.write(sender.getScreenName());
					}
					else {
						messages.remove(dm);
					}
				}
				else {
					messages.remove(dm);
				}
			}
			return messages;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Checks if the specified user is unsubscribed/blacklisted.
	 * @param user The user to test.
	 * @return Returns true or false indicating whether the user is unsubscribed.
	 */
	public static boolean isUserUnsubscribed(User user) {
		String name = user.getScreenName();
		List<String> blacklisted = FileUtils.getBlacklistedScreenNames();
		for (String s : blacklisted) {
			if (name.contentEquals(s)) {
				return true;
			}
		}
		return false;
	}
	
}
