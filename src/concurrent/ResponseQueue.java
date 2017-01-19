package concurrent;

import java.util.List;
import java.util.Vector;

public class ResponseQueue extends Thread {
	
	private List<TwitterResponse> queuedResponses;
	
	public ResponseQueue(TwitterResponse... responses) {
		queuedResponses = new Vector<TwitterResponse>();
		for (TwitterResponse r : responses) {
			try {
				r.start();
				queuedResponses.add(r);
			} catch (IllegalThreadStateException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
}
