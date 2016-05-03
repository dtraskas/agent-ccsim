package com.ccsim;

/*
 * CallHandler.java
 *
 * Created on January 3, 2008, 10:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import java.io.IOException;                                    
import java.text.*;
import java.util.*;
import java.io.Serializable;

/**
 *
 * @author dimitriostraskas
 */
public class CallHandler implements Serializable
{
    private Vector handleTimes;
    private Vector shifts;
     
    public CallHandler()
    {
        handleTimes = new Vector();
        shifts = new Vector();
    }
    
    public void allocateShift(Date start, Date finish, SkillGroup sg)
    {
        shifts.add(new PeriodShiftInfo(sg, start, finish));
    }
    
    public void setHandleTime(Date start, Date finish, HandleInfo hi)
    {
        int index = findPeriod(start, finish);
        PeriodHandleInfo period = null;
        if (index >= 0){
            period = (PeriodHandleInfo)this.handleTimes.get(index);
        } else {
            period = new PeriodHandleInfo(start, finish);
            handleTimes.add(period);
        }
        period.add(hi);
    }    
    
    public int findPeriod(Date start, Date finish)
    {
        for(int i=0; i<handleTimes.size(); i++){
            PeriodHandleInfo period = (PeriodHandleInfo)handleTimes.get(i);
            if (period.getStart() == start && period.getFinish() == finish){
                return i;
            }
        }
        return -1;
    }
   
    public SkillGroup getSkillGroup()
    {
        return ((PeriodShiftInfo)shifts.get(0)).getSkillgroup();
    }
    
    public Skill getSkill(int index)
    {
        return ((PeriodShiftInfo)shifts.get(0)).getSkillgroup().getSkillAt(index);
    }
    
    public int getSkillCount()
    {
        return ((PeriodShiftInfo)shifts.get(0)).getSkillgroup().getSkillCount();
    }
   
    public Vector getHandleTimes()
    {
        return this.handleTimes;
    }
    
    public Vector getShifts()
    {
        return this.shifts;
    }
}
