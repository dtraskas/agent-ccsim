package com.ccsim;

/*
 * PeriodCallInfo.java
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
 * This class holds all the call related information for every period in a day.
 */
public class PeriodCallInfo extends PeriodAbstract implements Serializable
{    
    private Vector callVolume;
    
    /** Initialises a new instance of PeriodCallInfo and sets everything to the default values.*/
    public PeriodCallInfo ()
    {
        this.callVolume = new Vector();
    }
   
    public boolean add(CallVolume cv)
    {
        return this.callVolume.add(cv);
    }

    public void remove(CallVolume cv)
    {
        this.callVolume.remove(cv);
    }
    
    public CallVolume getAt(int index)
    {
        return (CallVolume)this.callVolume.get(index);
    }
    
    public int getCount()
    {
        return this.callVolume.size();
    }
    
    public void clear()
    {
        this.callVolume.clear();
    }    
}
