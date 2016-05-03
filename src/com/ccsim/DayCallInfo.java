package com.ccsim;

/*
 * DayCallInfo.java
 *
 * Created on 03 Jan 2008, 21:00
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
public class DayCallInfo extends DayAbstract implements Serializable
{
    
    /** Initialises a new instance of DayCallInfo and sets everything to the default values.*/
    public DayCallInfo ()
    {
        
    }
    
    
    /** Initialises a new instance of DayCallInfo with the specified date. */
    public DayCallInfo(Date date)
    {
        super(date);
    }
    
    public boolean add(PeriodCallInfo period)
    {
        return this.periods.add(period);
    }
    
    public void remove(PeriodCallInfo period)
    {
        this.periods.remove(period);
    }
    
    public int findPeriod(Date p)
    {
        for (int i=0; i<this.periods.size();i++){
            PeriodCallInfo period = (PeriodCallInfo)this.periods.get(i);
            Date start = period.getStart();
            Date finish = period.getFinish();
            
            if (start.getTime() <= p.getTime() && p.getTime() < finish.getTime()) return i;
        }
        return -1;
    }
}
