package com.ccsim;

/*
 * Skill.java
 *
 * Created on 03 Jan 2008, 21:00
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;
import java.rmi.dgc.*;

/**
 *
 * This class holds all the information necessary to describe a skill
 * or otherwise the type of the call.
 */
public class Skill implements Serializable
{
    private String id;
    private String name;
    private int priority;
    /// this is the telephone service level factor, 
    /// the acceptable waiting time for a customer (usually 30 seconds).
    private int tsf;
    
    transient private final int DEFAULT_TSF = 30;

    /** Initialises a new instance of Skill and sets everything to the default values.*/
    public Skill ()
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.name = "";
        this.priority = 0;
        this.tsf = DEFAULT_TSF;
    }
    
    /** Initialises a new instance of Skill with the specified name.*/
    public Skill(String name)
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.name = name;
        this.priority = 0;
        this.tsf = DEFAULT_TSF;
    }
    
    /** Initialises a new instance of Skill with the specified id and name*/
    public Skill(String id, String name)
    {
        this.id = id;
        this.name = name;
        this.priority = 0;
        this.tsf = DEFAULT_TSF;
    }
    
    /// Returns a shallow copy of Skill (new GUID).
    public Skill copy()
    {
        Skill new_skill = new Skill(this.getName());
        new_skill.setPriority(this.getPriority());
        new_skill.setTsf(this.getTsf());
        return new_skill;
    }

    /// Gets the unique id of the skill.
    public String getId()
    {
        return this.id;
    }
    
    /// Gets the name of the skill.
    public String getName()
    {
        return this.name;
    }
    
    /// Gets the priority of the skill.
    public int getPriority()
    {
        return this.priority;
    }
    
    /// Sets the name of the skill.
    public void setName(String name)
    {
        this.name = name;
    }
    
    /// Sets the priority of the skill.
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /// Gets the tsf of the skill.
    public int getTsf()
    {
        return tsf;
    }

    /// Sets the tsf of the skill.
    public void setTsf(int tsf)
    {
        this.tsf = tsf;
    }
    
    /// Gets the name of the skill
    public String toString()
    {
        return this.getName();
    }
}
