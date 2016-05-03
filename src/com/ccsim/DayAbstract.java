package com.ccsim;

/*
 * DayAbstract.java
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
 * This is the base class used to derive classes that for example hold call information per period and day.
 */
abstract public class DayAbstract implements Serializable
{
    protected Date date;
    protected Vector periods;
    
    /** Initialises a new instance of DayAbstract and sets everything to the default values.*/
    public DayAbstract ()
    {
        this.date = new Date(0);
        this.periods = new Vector();
    }

    /** Initialises a new instance of DayAbstract with the specified date. */
    public DayAbstract(Date date)
    {
        this.date = date;
        this.periods = new Vector();
    }
    
    public Date getDate()
    {
        return this.date;
    }
    
    public void setDate(Date date)
    {
        this.date = date;
    }
    
    public boolean add(PeriodAbstract period)
    {
        return this.periods.add (period);
    }
    
    public void remove(PeriodAbstract period)
    {
        this.periods.remove(period);
    }
    
    public void removeAt(int index)
    {
        this.periods.remove(index);
    }
    
    public PeriodAbstract getAt(int index)
    {
        return (PeriodAbstract)this.periods.get(index);
    }
    
    public void clear()
    {
        this.periods.clear ();
    }
    
    public int getCount()
    {
        return this.periods.size();
    }
}

