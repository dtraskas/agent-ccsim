package com.ccsim;

/*
 * OpeningTimes.java
 *
 * Created on 07 Jan 2007, 22:28
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;
import java.util.*;

/**
 *
 * This class holds all the attributes of the opening time of a resource
 * such as a call centre or the business that owns the call centre.
 */
public class OpeningTimes implements Serializable
{
    private Date start;
    private Date finish;
    
    // Initialises a new instance of the OpeningTime class and sets everything to zero.
    public OpeningTimes ()
    {
        this.start = new Date(0);
        this.finish = new Date(0);
    }
    
    /// Initialises a new instance of the OpeningTime class and sets the opening and closing time.
    public OpeningTimes (Date start, Date finish)
    {
        this.start = start;
        this.finish = finish;
    }
    
    /// Gets the opening time of a Resource.
    public Date getStart()
    {
        return this.start;
    }
    
    /// Gets or sets the closing time of a Resource.
    public Date getFinish()
    {
        return this.finish;
    }
    
    /// Sets the opening time of a Resource.
    public void setStart(Date start)
    {
        this.start = start;
    }
   
    /// Sets the closing time of a Resource.
    public void setFinish(Date finish)
    {
        this.finish = finish;
    }
}
