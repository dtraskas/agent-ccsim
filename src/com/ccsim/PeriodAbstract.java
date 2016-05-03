package com.ccsim;

/*
 * PeriodAbstract.java
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
 * This class holds all the attributes of a time period such as a start and finish time.
 */
abstract public class PeriodAbstract implements Serializable
{    
   protected Date start;
   protected Date finish;

    /// The base class constructor only sets start and finish times to zero.
    public PeriodAbstract()
    {
        this.start = new Date(0);
        this.finish = new Date(0);
    }

    /// The base class constructor sets start and finish times to specified parameters.
    public PeriodAbstract(Date start, Date finish)
    {
        this.start = start;
        this.finish = finish;
    }

    /// Gets the start time.
    public Date getStart()
    {
        return this.start;
    }

    /// Sets the start time.
    public void setStart(Date start)
    {
        this.start = start;
    }
    
    /// Gets the finish time.
    public Date getFinish()
    {
        return this.finish;
    }

    /// Sets the finish time.
    public void setFinish(Date finish)
    {
        this.finish = finish;
    }
}
