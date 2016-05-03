package com.ccsim;

/*
 * SkillGroupInfo.java
 *
 * Created on February 3, 2008, 10:24 PM
 * Bath University
 *
 */
import java.io.Serializable;
import java.util.*;
/**
 *
 * @author dimitriostraskas
 */
public class SkillGroupInfo implements Serializable
{
    private SkillGroup skillGroup;
    private int fte;
    
    /** Creates a new instance of SkillGroupInfo */
    public SkillGroupInfo()
    {
        this.skillGroup = null;
        this.fte = 1;
    }
    
    /// Initialises a new instance of the HandleInfo class 
    /// where the Skill and distribution are specified.
    public SkillGroupInfo(SkillGroup skillGroup, int fte)
    {
        this.skillGroup = skillGroup;
        this.fte = fte;
    }

    public SkillGroup getSkillGroup()
    {
        return this.skillGroup;
    }

    public void setSkillGroup(SkillGroup skillGroup)
    {
        this.skillGroup = skillGroup;
    }
    
    public int getFTE()
    {
        return this.fte;
    }
    
    public void setFTE(int fte)
    {
        this.fte = fte;
    }
}
