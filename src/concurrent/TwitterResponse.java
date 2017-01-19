package concurrent;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterResponse extends Thread {
	
	private Twitter twitter;
	private Status inReplyTo;
	private final long ID;
	
	public TwitterResponse(Status inReplyTo, Twitter twitter) {
		ID = inReplyTo.getInReplyToStatusId();
		this.inReplyTo = inReplyTo;
		this.twitter = twitter;
	}
	
	@Override
	public void run() {
		try {
			if (!Toolbox.isUserUnsubscribed(inReplyTo.getUser())) {
				StatusUpdate update = Toolbox.getResponseFromCleverbot(inReplyTo); 
				twitter.updateStatus(update);
				Toolbox.updateLastId(ID); // FIXME Might be problematic
			}
			this.interrupt();
		} catch (TwitterException e) {
			e.printStackTrace();
			this.interrupt();
		}
	}
	
	@Override
	public long getId() {
		return ID;
	}
	
}

