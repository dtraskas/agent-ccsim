package com.ccsim;

/*
 * DayShiftInfo.java
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
 * This class holds all the shift related information per day and period.
 */
public class DayShiftInfo extends DayAbstract implements Serializable
{
    /** Initialises a new instance of DayShiftInfo and sets everything to the default values.*/
    public DayShiftInfo()
    {
    }
    
    /** Initialises a new instance of DayShiftInfo with the specified date. */
    public DayShiftInfo(Date date)
    {
        super(date);
    }
    
    public boolean add (PeriodSkillGroupInfo period)
    {
        return this.periods.add(period);
    }
    
    public void remove (PeriodSkillGroupInfo period)
    {
        this.periods.remove(period);
    }
    
    public int findPeriod(Date p)
    {
        for (int i=0; i<this.periods.size();i++){
            PeriodSkillGroupInfo period = (PeriodSkillGroupInfo)this.periods.get(i);
                if (period.getStart().getTime() == p.getTime()) return i;
        }
        return -1;
    }    
}