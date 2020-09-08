package dk.anaha.os.windows.test;

import java.util.logging.Level;

import dk.anaha.os.windows.EventLog;

/**
 * 
 * @author Azri Rosborg
 *
 */
public abstract class TestEventLog {
	public TestEventLog() {
	}

	public static void main(String[] args) throws Exception {
		try (EventLog el=new EventLog("JavaAppLoggingToWindowsEventLog")){
			el.log(Level.WARNING,"This is a warning");
		}
	}

}
