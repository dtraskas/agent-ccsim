package com.ccsim;

/*
 * CallVolume.java
 *
 * Created on 03 Jan 2008, 21:00
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;
import java.rmi.dgc.*;
import java.util.Date;

/**
 *
 * This class holds all the call related information such as type of skill and id.
 */
public class CallVolume implements Serializable
{
    private Skill skill;
    private int volume;
    
    /** Initialises a new instance of CallInfo and sets everything to the default values.*/
    public CallVolume ()
    {
        this.skill = null;
        this.volume = 0;
    }
    
    /// Initialises a new instance of the CallInfo class with id and Skill specified.
    public CallVolume(Skill skill, int volume)
    {
        this.skill = skill;
        this.volume = volume;
    }
    
    public Skill getSkill()
    {
        return this.skill;
    }

    public void setSkill(Skill skill)
    {
        this.skill = skill;
    }
    
    public void setVolume(int volume)
    {
        this.volume = volume;
    }
    
    public int getVolume()
    {
        return this.volume;
    }
}
