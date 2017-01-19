package main;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import it.sauronsoftware.feed4j.FeedIOException;
import it.sauronsoftware.feed4j.FeedParser;
import it.sauronsoftware.feed4j.FeedXMLParseException;
import it.sauronsoftware.feed4j.UnsupportedFeedException;
import it.sauronsoftware.feed4j.bean.Feed;

public class Test {
	
	private static ChatterBotFactory factory = new ChatterBotFactory();
	private static ChatterBot bot;
	private static ChatterBotSession session; 
	
	public static void main(String[] args) throws IndexOutOfBoundsException, Exception {
		bot = factory.create(ChatterBotType.CLEVERBOT);
		session = bot.createSession();
		
		Feed f = FeedParser.parse(new URL("http://rss.nytimes.com/services/xml/rss/nyt/Technology.xml"));
		f.getItem(0).getLink();
		for (int i = 0; i < f.getItemCount(); i++) {
			String title = f.getItem(i).getTitle();
			System.out.println(title + ">>" + session.think(title));
		}
	}

}
