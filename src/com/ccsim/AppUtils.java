package com.ccsim;

/*
 * AppUtils.java
 *
 * Created on 6 Jan 2008, 19:07
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.File;
import java.util.*;
import java.net.URL;

/**
 *
 * General purpose utility functions
 */
public class AppUtils
{
    public enum State {SUCCESS, FAILURE};
    public enum LogType {ERROR, WARNING};
    
    /** Initialises a new instance of AppUtils and sets everything to the default values.*/
    public AppUtils() {}
    
    public static String getAppName() {return "acdmanager";}
    
    public static String getErrorLogName() {return "acdmanager_error.log";}
    
    public static String getExtension(File f) 
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
     
    /// Given a full path with filename it extracts the filename.
    public static String getFilename(String path)
    {        
        int index1 = path.lastIndexOf("\\") + 1;
        int index2 = path.lastIndexOf(".");
        return path.substring(index1, index2);
    }
    
    public static int getPeriods(Date start, Date finish, int intervalMinutes)
    {
        long diff = (finish.getTime() - start.getTime()) / (60 * 1000);
        int periods = (int)(diff / intervalMinutes);
        return periods;
    }
    
    public static Date getPeriod(Date open, int intervalMinutes, int periodIndex)
    {
        Calendar cal = new GregorianCalendar();
        cal.setTime(open);                
        cal.add(cal.MINUTE, (intervalMinutes*periodIndex));
        return cal.getTime();
    }
    
    public static int getPeriodIndex(Date open, Date current, int intervalMinutes)
    {
        Calendar cal1 = new GregorianCalendar();
        cal1.setTime(open);
        
        Calendar cal2 = new GregorianCalendar();
        cal2.setTime(current);
        
        int opening_time = cal1.get(cal1.HOUR_OF_DAY) * 60 + cal1.get(cal1.MINUTE);
        int current_time = cal2.get(cal1.HOUR_OF_DAY) * 60 + cal2.get(cal1.MINUTE);

        if (opening_time <= current_time){
            return ((current_time - opening_time) / intervalMinutes);
        } else {					
            return (((1440-opening_time) + current_time) / intervalMinutes);
        }
    }
}