package com.ccsim;

/*
 * PeriodHandleInfo.java
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
 * This class holds all the handle time related information for every period in a day.
 */
public class PeriodHandleInfo extends PeriodAbstract implements Serializable
{
    private Vector handleInfo;
    
    /** Initialises a new instance of PeriodHandleInfo and sets everything to the default values.*/
    public PeriodHandleInfo ()
    {
        this.handleInfo = new Vector();
    }
   
    /** Initialises a new instance of PeriodHandleInfo with the specified start and finish. */
    public PeriodHandleInfo (Date start, Date finish)
    {
        super(start, finish);
        handleInfo = new Vector();
    }
    
    public boolean add(HandleInfo hi)
    {
        return this.handleInfo.add(hi);
    }
    
    public void remove(HandleInfo hi)
    {
        this.handleInfo.remove(hi);
    }
    
    public HandleInfo getAt(int index)
    {
        return (HandleInfo)this.handleInfo.get(index);
    }
    
    public HandleInfo getAt(Skill skill)
    {
        for(Enumeration e = handleInfo.elements (); e.hasMoreElements ();){
            HandleInfo hi = (HandleInfo)e.nextElement();
            if (hi.getSkill().getId().compareTo(skill.getId()) == 0) return hi;
        }
        return null;
    }
    
    /// Returns the total number of handle info instances.
    public int getCount()
    {
        return this.handleInfo.size();
    }
    
    /// Removes all handle info instances from the period.
    public void clear()
    {
        this.handleInfo.clear();
    }
}