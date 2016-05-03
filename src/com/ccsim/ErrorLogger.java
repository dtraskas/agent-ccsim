package com.ccsim;

/*
 * ErrorLogger.java
 *
 * Created on 16 May 2007, 22:41
 *
 * Author: Dimitrios Traskas
 * Open University
 *
 */

import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.JOptionPane;

/**
 *
 *
 */
public abstract class ErrorLogger
{
    private static boolean on = true; 
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.dd.MM HH:mm"); 
    private static String appName = "";
    private static String errorLogName = "";
    
    /** Initialises a new instance of ErrorLogger and sets everything to the default values.*/
    public ErrorLogger() {}
    
    /// Return true if logging is on.
    public static boolean isOn() 
    {
        return on;
    }

   /// Set logging on/off.
   public static void setOn(boolean isOn) 
   {
        on = isOn;
   }

   public static void setApp(String name)
   {
        appName = name;
   }
   
   public static void setLogName(String name)
   {
        errorLogName = name;
   }
   
   /// Log the given message.
   public static void log(String msg, Exception ex, AppUtils.LogType type) 
   {
      if (on) {
         try {
            PrintStream logFile = new PrintStream(new FileOutputStream(errorLogName, true));            
            
            try {
                logFile.println("----------------------------------------------------------------------------------");
                logFile.println(dateFormat.format(new Date()) + " " + ex.getMessage());
                ex.printStackTrace(logFile);
                logFile.println("----------------------------------------------------------------------------------");
                
                if (type == AppUtils.LogType.ERROR){
                    JOptionPane.showMessageDialog(null, msg + "\n" + ex.getMessage(), appName, JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, msg + "\n" + ex.getMessage(), appName, JOptionPane.WARNING_MESSAGE);
                }
            } finally {
               logFile.close();
            }
         } catch(IOException ioex) {
            ioex.printStackTrace();
         }
      }
   }
}