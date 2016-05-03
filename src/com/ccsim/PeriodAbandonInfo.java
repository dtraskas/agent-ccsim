package com.ccsim;

/*
 * PeriodAbandonInfo.java
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
 * This class holds all the abandon time related information for every period in a day.
 */
public class PeriodAbandonInfo extends PeriodAbstract implements Serializable
{
    private Vector abandonInfo;
    
    /** Initialises a new instance of PeriodAbandonInfo and sets everything to the default values.*/
    public PeriodAbandonInfo ()
    {
        this.abandonInfo = new Vector();
    }
   
    /** Initialises a new instance of PeriodAbandonInfo with the specified start and finish. */
    public PeriodAbandonInfo (Date start, Date finish)
    {
        super(start, finish);
        abandonInfo = new Vector();
    }
    
    public boolean add(AbandonInfo ai)
    {
        return this.abandonInfo.add(ai);
    }
    
    public void remove(AbandonInfo ai)
    {
        this.abandonInfo.remove(ai);
    }
    
    public int find(Skill skill)
    {
        for(int i=0; i<abandonInfo.size(); i++){
            AbandonInfo ai = (AbandonInfo)abandonInfo.get(i);
            if (ai.getSkill().getId().compareTo(skill.getId()) == 0) return i;
        }
        return -1;
    }
    
    public AbandonInfo getAt(int index)
    {
        return (AbandonInfo)this.abandonInfo.get(index);
    }
    
    public void removeAt(int index)
    {
        this.abandonInfo.remove(index);
    }
    
    public int getCount()
    {
        return this.abandonInfo.size();
    }
    
    public void clear()
    {
        this.abandonInfo.clear();
    }
}
