package elements;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class AuthenucationTest {

	private static final String CONSUMER_KEY = "TJxsh4CCCAnH0gs5nusxNGTVE";
	private static final String CONSUMER_SECRET = "bZIFmy1jToFv9IvUxQKcIu0vNMSqMT2TcqSGDgHwbpyNoEtujZ";
	
	public static ActionListener pinReciever; 
	
	private static long PIN = 0;
	static EnterPinUI temp = new EnterPinUI();
	static Twitter twitter;
	static RequestToken requestToken;
	static AccessToken accessToken;
	
	public static void register() throws Exception {
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(CONSUMER_KEY);
		cb.setOAuthConsumerSecret(CONSUMER_SECRET);
		cb.setOAuthAccessToken(null);
		cb.setOAuthAccessTokenSecret(null);

		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();
		
		temp.setVisible(true);
		
		requestToken = twitter.getOAuthRequestToken();
		
		pinReciever = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				PIN = temp.PIN;
				accessToken = null;
				try {
					if (PIN != 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, "" + PIN);
					} else {
						accessToken = twitter.getOAuthAccessToken();
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}
		};
		
		while (null == accessToken) {

			String command = "cmd /c start " + requestToken.getAuthorizationURL();
			Process child = Runtime.getRuntime().exec(command);
			try {
				if (PIN > 0) {
					accessToken = twitter.getOAuthAccessToken(requestToken, "" + PIN);
				} else {
					accessToken = twitter.getOAuthAccessToken();
				}
			} catch (TwitterException te) {
				if (401 == te.getStatusCode()) {
					System.out.println("Unable to get the access token.");
				} else {
					te.printStackTrace();
				}
			}
		}
		FileUtils.write(accessToken.getToken(), accessToken.getTokenSecret());
	}

}
