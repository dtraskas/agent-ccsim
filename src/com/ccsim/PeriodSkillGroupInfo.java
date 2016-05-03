package com.ccsim;

/*
 * PeriodSkillGroupInfo.java
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
 * This class holds all the shift related information for every period in a day
 * eg: handler Fred Blogs handles calls of type A between 10:00 in the morning and 18:00 in the evening.
 */
public class PeriodSkillGroupInfo extends PeriodAbstract implements Serializable
{
    private Vector skillGroupInfo;
    
    /**
     * Initialises a new instance of PeriodSkillGroupInfo and sets everything to the default values.
     */
    public PeriodSkillGroupInfo()
    {
        this.skillGroupInfo = new Vector();
    }    
    
    /**
     * Initialises a new instance of PeriodSkillGroupInfo and sets the skillgroup to the specified skillgroup.
     */
    public PeriodSkillGroupInfo(SkillGroup skillgroup, Date start, Date finish)
    {
        super(start, finish);
        this.skillGroupInfo = new Vector();
    }

    public boolean add(SkillGroupInfo sgi)
    {
        return this.skillGroupInfo.add(sgi);
    }
    
    public void remove(SkillGroupInfo sgi)
    {
        this.skillGroupInfo.remove(sgi);
    }
    
    public SkillGroupInfo getAt(int index)
    {
        return (SkillGroupInfo)this.skillGroupInfo.get(index);
    }
    
    public SkillGroupInfo getAt(SkillGroup sg)
    {
        for(Enumeration e = skillGroupInfo.elements (); e.hasMoreElements ();){
            SkillGroupInfo sgi = (SkillGroupInfo)e.nextElement();
            if (sgi.getSkillGroup().getId().compareTo(sg.getId()) == 0) return sgi;
        }
        return null;
    }
    
    public int getTotalFTECount()
    {
        int count = 0;
        for(Enumeration e = skillGroupInfo.elements (); e.hasMoreElements ();){
            SkillGroupInfo sgi = (SkillGroupInfo)e.nextElement();
            count += sgi.getFTE();
        }
        return count;
    }
    
    /// Returns the total number of handle info instances.
    public int getCount()
    {
        return this.skillGroupInfo.size();
    }
    
    /// Removes all handle info instances from the period.
    public void clear()
    {
        this.skillGroupInfo.clear();
    }    
}
