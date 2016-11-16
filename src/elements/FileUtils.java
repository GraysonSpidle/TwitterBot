package elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import com.google.appengine.repackaged.com.google.common.io.Files;

import main.Main;
import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class FileUtils {
	
	private static final String CONSUMER_KEY = "nE73hD0zYynB3ys7jKocbOA2C";
	private static final String CONSUMER_SECRET = "vQvzq9anBNDA5RXDqbmLrA5gC83R1RlsXZwauYXMbcjBRzztlH";
	
	public static void write(String userScreenName) { 
		try (PrintStream out = new PrintStream(new FileOutputStream("blacklist.txt"))) {
			out.print(userScreenName);
			out.println();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		
	}
	
	public static void write(String accessKey, String accessKeySecret) {
		try (PrintStream out = new PrintStream(new FileOutputStream("accessKeys.txt"))) {
			    out.println(accessKey);
			    out.println(accessKeySecret);
		} catch (FileNotFoundException e1) {
			System.err.println("Unable to find the the file.");
			e1.printStackTrace();
		}
		
	}
	
	public static void write(long id) {
		try (PrintStream out = new PrintStream(new FileOutputStream("lastID.txt"))) {
		    out.print(id);
		    System.out.println("Successfully updated the previous id");
		} catch (FileNotFoundException e1) {
			System.err.println("Unable to find the the file.");
			e1.printStackTrace();
		}
	}
	
	public static long getLastRecentReplyID() {
		try {
			List<String> output = Files.readLines(new File("lastID.txt"), Charset.defaultCharset());
			return Long.parseLong(output.get(0));
		} catch (IOException e) {
			return -1;
		} 
	}
	
	public static List<String> getBlacklistedScreenNames() {
		try {
			List<String> output = Files.readLines(new File("blacklist.txt"), Charset.defaultCharset()); 
			output.add(Main.BOT_HANDLE);
			return output;
		} catch (IOException e) {
			return null;
		}
		
	}
	
	public static String getKey() throws ArrayIndexOutOfBoundsException {
		try {
			List<String> lines = Files.readLines(new File("accessKeys.txt"), Charset.defaultCharset());
			return lines.get(0);
		} catch (IOException e) {
			System.err.println("Unable to find the the file.");
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getSecret() {
		try {
			List<String> lines = Files.readLines(new File("accessKeys.txt"), Charset.defaultCharset());
			return lines.get(1);
		} catch (IOException e) {
			System.err.println("Unable to find the the file.");
			e.printStackTrace();
		}
		return null;
	}
	
}
