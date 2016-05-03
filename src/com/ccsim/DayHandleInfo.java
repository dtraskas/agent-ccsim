package com.ccsim;

/*
 * DayHandleInfo.java
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
 * This class holds all the handle time related information per day and period.
 */
public class DayHandleInfo extends DayAbstract implements Serializable
{
    /** Initialises a new instance of DayHandleInfo and sets everything to the default values.*/
    public DayHandleInfo ()
    {
    }
    
    /** Initialises a new instance of DayHandleInfo with the specified date. */
    public DayHandleInfo(Date date)
    {
        super(date);
    }

    public boolean add (PeriodHandleInfo period)
    {
        return this.periods.add(period);
    }
    
    public void remove (PeriodHandleInfo period)
    {
        this.periods.remove(period);
    }
    
    public int findPeriod(Date p)
    {
        for (int i=0; i<this.periods.size();i++){
            PeriodHandleInfo period = (PeriodHandleInfo)this.periods.get(i);
                if (period.getStart().getTime() == p.getTime ()) return i;
        }
        return -1;
    }
}
