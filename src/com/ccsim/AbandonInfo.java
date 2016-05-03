package com.ccsim;

/*
 * AbandonInfo.java
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
 *
 */
public class AbandonInfo implements Serializable
{
    private Skill skill;
    private DistributionAbstract abandonTime;
    
    /** Initialises a new instance of AbandonInfo and sets everything to the default values.*/
    public AbandonInfo ()
    {
        this.skill = null;
        this.abandonTime = null;
    }
    
    /// Initialises a new instance of the AbandonInfo class 
    /// where the Skill and distribution are specified.
    public AbandonInfo(Skill skill, DistributionAbstract abandonTime)
    {
        this.skill = skill;
        this.abandonTime = abandonTime;
    }

    public Skill getSkill()
    {
        return this.skill;
    }

    public void setSkill(Skill skill)
    {
        this.skill = skill;
    }
    
    public DistributionAbstract getAbandonTime()
    {
        return this.abandonTime;
    }
    
    public void setAbandonTime(DistributionAbstract abandonTime)
    {
        this.abandonTime = abandonTime;
    }
}
