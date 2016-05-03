package com.ccsim;

/*
 * PeriodShiftInfo.java
 *
 * Created on February 3, 2008, 10:49 PM
 * Bath University
 *
 */

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author dimitriostraskas
 */
public class PeriodShiftInfo extends PeriodAbstract implements Serializable
{
    private SkillGroup skillgroup;
    
    /**
     * Initialises a new instance of PeriodSkillGroupInfo and sets everything to the default values.
     */
    public PeriodShiftInfo()
    {
        this.skillgroup = null;
    }    
    
    /**
     * Initialises a new instance of PeriodSkillGroupInfo and sets the skillgroup to the specified skillgroup.
     */
    public PeriodShiftInfo(SkillGroup skillgroup, Date start, Date finish)
    {
        super(start, finish);
        this.skillgroup = skillgroup;
    }

    public void setSkillGroup(SkillGroup sg)
    {
        this.skillgroup = sg;
    }
    
    public SkillGroup getSkillgroup()
    {
        return this.skillgroup;
    }
}
