package com.ccsim;

/*
 * HandlerPacket.java
 *
 * Created on January 24, 2008, 9:44 PM
 * Bath University
 *
 */

import java.io.Serializable;
import java.util.*;
/**
 *
 * @author dimitriostraskas
 */
public class HandlerPacket implements Serializable
{
    private String handlerID;
    private Date nextAvailable;
    private Date shiftEnd;
    private SkillGroup skillgroup;
        
    public HandlerPacket(String handlerID, Date nextAvailable, Date shiftEnd, SkillGroup skillgroup)
    {
        this.handlerID = handlerID;
        this.nextAvailable = nextAvailable;
        this.shiftEnd = shiftEnd;
        this.skillgroup = skillgroup;
    }

    public String getHandlerID()
    {
        return this.handlerID;
    }

    public Date getNextAvailable()
    {
        return this.nextAvailable;
    }    
    
    public Date getShiftEnd()
    {
        return this.shiftEnd;
    }
    
    public SkillGroup getSkillGroup()
    {
        return this.skillgroup;
    }
}
