package concurrent;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class Test {

	public static void main(String[] args) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 30);
		cal.set(Calendar.MONTH, 11);
		cal.set(Calendar.YEAR, 2016);
//		String date = String.valueOf(day) + "/" + String.valueOf(month) + "/" + String.valueOf(year);
//		System.out.println(date);
		Calendar cal2 = advanceAWeek(cal);

		System.out.println(cal.get(Calendar.DAY_OF_YEAR) + ":" + cal.get(Calendar.YEAR));
		System.out.println(cal.getTime());
		System.out.println(cal2.getTime());
		
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
	
}
