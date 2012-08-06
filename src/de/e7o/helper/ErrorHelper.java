package de.e7o.helper;

import java.util.LinkedList;

public class ErrorHelper {
	public static LinkedList<String> errMessages;
	public static String lastErr = "";
	
	static {
		errMessages = new LinkedList<String>();
	}
	
	public static void addErrMessage(Exception exc) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : exc.getStackTrace()) {
			sb.append("\n"+ste.getFileName()+"["+ste.getLineNumber()+"]."+ste.getMethodName());
		}
		addErrMessage(exc.getClass().getName()+": "+exc.getMessage()+"\n"+exc.getLocalizedMessage()+"\nStack Trace:"+sb.toString());
	}
	
	public static void addErrMessage(String err) {
		errMessages.add("["+TimeHelper.currentTime().toLocaleString()+"] "+err);
		lastErr = err;
	}
}
