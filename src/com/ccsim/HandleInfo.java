package com.ccsim;

/*
 * HandleInfo.java
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
 * This class holds the type of call being handled and the time it takes to handle it.
 */
public class HandleInfo implements Serializable
{
    private Skill skill;
    private DistributionAbstract handleTime;
    
    /** Initialises a new instance of HandleInfo and sets everything to the default values.*/
    public HandleInfo ()
    {
        this.skill = null;
        this.handleTime = null;
    }
    
    /// Initialises a new instance of the HandleInfo class 
    /// where the Skill and distribution are specified.
    public HandleInfo(Skill skill, DistributionAbstract handleTime)
    {
        this.skill = skill;
        this.handleTime = handleTime;
    }

    public Skill getSkill()
    {
        return this.skill;
    }

    public void setSkill(Skill skill)
    {
        this.skill = skill;
    }
    
    public DistributionAbstract getHandleTime()
    {
        return this.handleTime;
    }
    
    public void setHandleTime(DistributionAbstract handleTime)
    {
        this.handleTime = handleTime;
    }
}
