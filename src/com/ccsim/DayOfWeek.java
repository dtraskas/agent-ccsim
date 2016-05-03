package com.ccsim;

/*
 * DayOfWeek.java
 *
 * Created on 07 Jan 2007, 22:35
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;

/**
 *
 *
 */
public enum DayOfWeek implements Serializable
{
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7);
    
    private int day;

    DayOfWeek(int day)
    {
        this.day = day;
    }

    public int day()
    {
        return this.day;
    }        
}