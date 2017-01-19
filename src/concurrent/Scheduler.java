package concurrent;

import java.util.List;
import java.util.Vector;

import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;

public class Scheduler extends Thread {
	
	private ResponseQueue qs;
	private Twitter twitter;
	private LimitType type;
	private List<TwitterResponse> threads;
	
	public Scheduler(Twitter twitter, LimitType type) {
		threads = new Vector<TwitterResponse>();
		this.twitter = twitter;
		this.type = type;
	}
	
	@Override
	public void run() {
		if (type.equals(LimitType.HOME_TIMELINE)) {
			RateLimitStatus limit = null;
			while (!this.isInterrupted()) {
				limit = Toolbox.getLimit("/statuses/home_timeline", twitter);
				if (limit.getRemaining() > 0) {
					List<Status> list = Toolbox.getTweetsFromHomeTimeline(twitter);
					if (list.size() > limit.getRemaining()) {
						list = list.subList(0, limit.getRemaining());
					}
					else {
						for (Status s : list) {
							TwitterResponse r = new TwitterResponse(s, twitter);
							r.start();
							threads.add(r);
						}
					}
				}
				else if (limit.getRemaining() == 0) {
					try {
						long amount = limit.getSecondsUntilReset() * 1000 + 1;
						System.out.println("Sleeping for " + String.valueOf(amount) + " milliseconds.");
						Thread.sleep(amount);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
				else {
					System.err.println("An error occurred exiting...");
					System.exit(0);
				}
			}
		}
		else if (type.equals(LimitType.MENTIONS_TIMELINE)) {
			RateLimitStatus limit = null;
			while (!this.isInterrupted()) {
				limit = Toolbox.getLimit("/statuses/mentions_timeline", twitter);
				if (limit.getRemaining() > 0) {
					List<Status> list = Toolbox.getTweetsFromMentionsTimeline(twitter);
					if (list.size() > limit.getRemaining()) {
						list = list.subList(0, limit.getRemaining());
					}
					else {
						for (Status s : list) {
							TwitterResponse r = new TwitterResponse(s, twitter);
							r.start();
							threads.add(r);
						}
					}
				}
				else if (limit.getRemaining() == 0) {
					try {
						long amount = limit.getSecondsUntilReset() * 1000 + 1;
						System.out.println("Sleeping for " + String.valueOf(amount) + " milliseconds.");
						Thread.sleep(amount);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
				else {
					System.err.println("An error occurred exiting...");
					System.exit(0);
				}
			}
		}
		else {
			this.interrupt();
		}
	}
	
	public List<TwitterResponse> getThreads() {
		return threads;
	}
	
	
}
