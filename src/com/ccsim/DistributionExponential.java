package com.ccsim;


/*
 * DistributionExponential.java
 *
 * Created on January 9, 2008, 11:36 PM
 * Bath University
 *
 */

import java.util.Random;
import java.io.Serializable;

/**
 *
 * @author dimitriostraskas
 */
public class DistributionExponential extends DistributionAbstract implements Serializable
{
    private double mean;
    private Random rnd;
    
    /** Creates a new instance of DistributionExponential */
    public DistributionExponential(double mean)
    {
        this.mean = mean;
        this.rnd = new Random();
    }   
    
    public double getRandom()
    {        
        double r = rnd.nextDouble();
        
        return (-1.0 * Math.log(1-r) * mean);
    }
    
    public double getMean()
    {
        return this.mean;
    }
}
