package com.ccsim;

/*
 * DayAbandonInfo.java
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
 * This class holds all the call related information per day and period.
 */
public class DayAbandonInfo extends DayAbstract implements Serializable
{
    /** Initialises a new instance of DayAbandonInfo and sets everything to the default values.*/
    public DayAbandonInfo ()
    {
    }
    
    /** Initialises a new instance of DayAbandonInfo with the specified date. */
    public DayAbandonInfo(Date date)
    {
        super(date);
    }
    
    public boolean add(PeriodAbandonInfo period)
    {
        return this.periods.add(period);
    }
    
    public void remove(PeriodAbandonInfo period)
    {
        this.periods.remove(period);
    }
    
    public int findPeriod(Date p)
    {
        for (int i=0; i<this.periods.size();i++){
            PeriodAbandonInfo period = (PeriodAbandonInfo)this.periods.get(i);
                if (period.getStart().getTime() == p.getTime ()) return i;
        }
        return -1;
    }
}
