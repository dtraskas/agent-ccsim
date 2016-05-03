package com.ccsim;

/*
 * DistributionValue.java
 *
 * Created on 07 Jan 2007, 22:18
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;

/**
 *
 * This class holds the attributes of a Fixed Distribution which is a single value.
 */
public class DistributionValue extends DistributionAbstract implements Serializable
{
    private double val;
    
    /**
     * Initialises a new instance of DistributionValue and sets everything to the default values.
     */
    public DistributionValue (double val)
    {
        this.val = val;
    }

    public double getValue()
    {
        return this.val;
    }
    
    public void setValue(double val)
    {
        this.val = val;
    }
}
