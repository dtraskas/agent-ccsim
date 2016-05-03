package com.ccsim;

/*
 * DistributionAbstract.java
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
 * This is the base class which represents a basic probability distribution.
 * All probability distribution types can be derived from it.
 */
abstract public class DistributionAbstract implements Serializable
{
    
    /** Initialises a new instance of DistributionAbstract and sets everything to the default values.*/
    public DistributionAbstract ()
    {
        // To be implemented by the derived class.
    }
}
