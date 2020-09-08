package dk.anaha.os.windows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.MissingResourceException;
import java.util.logging.Level;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.PSID;

/*
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 */

/**
 * 
 * @author Azri Rosborg azri.rosborg[at]hotmail.com
 * date: 8th September 2020
 *
 */
public class EventLog  implements AutoCloseable {
	private final Advapi32 advapi32 = Advapi32.INSTANCE;
	private HANDLE windowsEventLog = null;
	
	public enum wType {
		EVENTLOG_SUCCESS(0x0000),
		EVENTLOG_AUDIT_FAILURE(0x0010),
		EVENTLOG_AUDIT_SUCCESS(0x0008),
		EVENTLOG_ERROR_TYPE(0x0001),
		EVENTLOG_INFORMATION_TYPE(0x0004),
		EVENTLOG_WARNING_TYPE(0x0002);
	    private final int type;

	    private wType(int value) {
	        this.type = value;
	    }
	    public int getValue(){
	    	return type;
	    }
	};
	public enum dwEventID {
		Success(0x00000000),
		Informational(0x40000000),
		Warning(0x80000000),
		Error(0xC0000000),
		
		SystemCode(0x00000000),
		CustomerCode(0x200000);
		
		private int eventId;
		private dwEventID(int eventId){
			this.eventId=eventId;
		}
		public int getValue(){
			return this.eventId;
		}
		public void setFacilityCode(int facility){
			facility=facility & 0xfff;
			facility=facility<<16;
			eventId=eventId|facility;
		}
		public void setFacilityStatus(int status){
			eventId=eventId|(status & 0xFFFF);
		}
		
	}
	public EventLog(String logName){
		windowsEventLog=advapi32.RegisterEventSource(null,logName);
		if (windowsEventLog.getPointer()==Pointer.NULL)
			throw new MissingResourceException(Kernel32Util.formatMessage(Native.getLastError()),"advapi32.dll","Microsoft Windows"); 
	}
	public void info(String message) {
		try {
			log(Level.INFO,message);
		} catch (Exception e) {
		}
	}
	public void warning(String message) {
		try {
			log(Level.WARNING,message);
		} catch (Exception e) {
		}
	}
	public void severe(String message) {
		try {
			log(Level.SEVERE,message);
		} catch (Exception e) {
		}
	}
	public void log(Level severity,String message,Throwable thrown) throws Exception{
		StringBuffer sb=new StringBuffer();
		StringWriter sw = new StringWriter();
		thrown.printStackTrace(new PrintWriter(sw));
		sb.append(message)
		  .append("\n")
		  .append(thrown.getClass().getCanonicalName())
		  .append("\n")
		  .append(thrown.getMessage())
		   .append("\n")
		  .append(sw.toString());
		log(severity,sb.toString());
	}	
	public void log(Level severity,String message) throws Exception  {
		if (windowsEventLog!=null){
			
			wType type = wType.EVENTLOG_INFORMATION_TYPE;
			if (severity==Level.FINE || severity==Level.FINER || severity==Level.FINEST || severity==Level.INFO)
				type = wType.EVENTLOG_INFORMATION_TYPE;
			if (severity==Level.WARNING)
				type = wType.EVENTLOG_WARNING_TYPE;
			if (severity==Level.SEVERE)
				type = wType.EVENTLOG_ERROR_TYPE;
			
			int wCategory = 0x00;
			dwEventID eventID = dwEventID.Informational;
			PSID usersid=null;
			int wNumStrings = 1;
			int dwDataSize = 0;
			String[] strings=new String[1];
			if (message.length()>31839)
				message=message.substring(0,31839-1);
			strings[0]=message;
			Pointer rawData=Pointer.NULL;
			
			boolean ok=advapi32.ReportEvent(windowsEventLog, type.getValue(), wCategory, eventID.getValue(), usersid, wNumStrings, dwDataSize, strings, rawData);
			if (!ok){
				int dwErrorCode=Native.getLastError();
				throw new Exception(Kernel32Util.formatMessage(dwErrorCode));
			}
		}
	}
	@Override
	public void close() throws Exception {
		if (windowsEventLog!=null){
			boolean done=advapi32.DeregisterEventSource(windowsEventLog);
			if (!done){
				int dwErrorCode=Native.getLastError();
				throw new Exception(Kernel32Util.formatMessage(dwErrorCode));
			}
		}else
			throw new Exception("No connection to Windows event log.");
	}
}