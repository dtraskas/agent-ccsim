package com.ccsim;

/*
 * SkillGroup.java
 *
 * Created on 07 Jan 2007, 22:18
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.Serializable;
import java.util.*;
import java.rmi.dgc.*;

/**
 *
 * This class holds all the attributes of a skill group, a collection of skills.
 */
public class SkillGroup implements Serializable
{
    private String id;
    private String name;
    private Vector skills;
    private int index = 0;
    
    
    /** Initialises a new instance of SkillGroup and sets everything to the default values.*/
    public SkillGroup ()
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.name = "";
        this.skills = new Vector();
    }
    
    /** Initialises a new instance of SkillGroup with the specified name.*/
    public SkillGroup (String name)
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.name = name;
        this.skills = new Vector();
    }    
       
    /** Initialises a new instance of SkillGroup with the specified id and name.*/
    public SkillGroup (String id, String name)
    {
        this.id = id;
        this.name = name;
        this.skills = new Vector();
    }
    
    /// Returns true if the skill is added succesfully
    public boolean addSkill(Skill skill)
    {
        return this.skills.add(skill);
    }
    
    /// Removes the specified skill.
    public void removeSkill(Skill skill)
    {
        this.skills.remove(skill);
    }
    
    /// Removes the skill at the specified position.
    public void removeSkillAt(int index)
    {
        this.skills.remove(index);
    }
    
    /// Returns the number of skills in the collection.
    public int getSkillCount()
    {
        return this.skills.size();
    }
    
    /// Removes all skills from the collection.
    public void clearSkills()
    {
        this.skills.clear();
    }
    
    /// Searches for the specified Skill instance by using the specified name.
    public int findByName(String name)
    {
        for(int i=0; i<this.getSkillCount(); i++){
            if (name == ((Skill)this.skills.get(i)).getName()) 
                return i; 
        }
        return -1;
    }

    /// Searches for the specified Skill instance by using the specified id.
    public int findById(String id)
    {
         for(int i=0; i<this.getSkillCount(); i++){
            if (id == ((Skill)this.skills.get(i)).getId()) 
                return i; 
        }
        return -1;
    }
    
    /// Returns the skill at the specified index.
    public Skill getSkillAt(int index)
    {
        return (Skill)this.skills.get(index);
    }
    
    /// Gets the unique id of the skillgroup.
    public String getId()
    {
        return this.id;
    }
    
    /// Gets the name of the skillgroup.
    public String getName()
    {
        return this.name;
    }
    
    /// Sets the name of the skillgroup.
    public void setName(String name)
    {
        this.name = name;
    }
    
    /// Gets the list of skills.
    public Vector getSkills()
    {
        return this.skills;
    }
   
    /// Sets the list of skills.
    public void setSkills(Vector skills)
    {
        this.skills = skills;
    }
    
    public String toString()
    {
        return this.name;
    }
}
