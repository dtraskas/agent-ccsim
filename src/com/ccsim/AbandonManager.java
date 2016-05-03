package com.ccsim;

/*
 * AbandonManager.java
 *
* Created on 07 Jan 2007, 22:18
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;
import java.util.*;

/**
 *
 * This class handles abandonment information.
 */
public class AbandonManager implements Serializable
{
    private Vector days;
    
    /** Initialises a new instance of AbandonManager and sets everything to the default values.*/
    public AbandonManager ()
    {
        this.days = new Vector();
    }
    
    /// Adds a day instance to the collection.
    public boolean add(DayAbandonInfo day)
    {
        return this.days.add (day);
    }
    
    /// Removes a day instance from the collection.
    public void remove(DayAbandonInfo day)
    {
        this.days.remove(day);
    }
    
    /// Removes a day instance from the collection at the specified instance.
    public void removeAt(int index)
    {
        this.days.remove(index);
    }
    
    /// Returns instance of DayAbandonInfo at specified index
    public DayAbandonInfo getAt(int index)
    {
        return (DayAbandonInfo)this.days.get(index);
    }
    
    /// Removes all days.
    public void clear()
    {
        this.days.clear();
    }
    
    /// Returns instance of DayAbandonInfo at specified index
    public DayAbandonInfo getDayAbandonInfoAt(int index)
    {
        return (DayAbandonInfo)this.days.get(index);
    }
    
    /// Returns instance of DayAbandonInfo at the specified date.
    public DayAbandonInfo getDayAbandonInfoAt(Date date)
    {
        for(Enumeration e = days.elements (); e.hasMoreElements ();){
            DayAbandonInfo day = ((DayAbandonInfo)e.nextElement());
            if (day.getDate().getTime() == date.getTime()) return day;
        }
        return null;
    }
    
    /// Returns the number of days in the collection.
    public int getCount()
    {
        return this.days.size();
    }
}
