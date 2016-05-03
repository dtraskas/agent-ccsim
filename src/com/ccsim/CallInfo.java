package com.ccsim;

/*
 * CallInfo.java
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
import java.util.Vector;

/**
 *
 * This class holds all the call related information such as type of skill and id.
 */
public class CallInfo implements Serializable
{
    private String id;
    private Skill skill;
    private String handlerID;
    private Vector callCentres;
    private int run;
    private Date dtArrival;
    private Date dtCurrent;
    
    private double answerTime;
    private double handleTime;
    private double abandonTime;
    
    private Vector globalRoutingLog;
    private Vector localRoutingLog;
    
    private boolean isLastCall;
    public int ttl;
    
    /** Initialises a new instance of CallInfo and sets everything to the default values.*/
    public CallInfo ()
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.skill = null;        
        this.handlerID = "";
        this.callCentres = new Vector();
        
        this.run = 1;
        this.dtArrival = new Date(0);
        this.dtCurrent = new Date(0);
        this.answerTime = 0;
        this.handleTime = 0;
        this.abandonTime = 0;
        
        this.ttl = 0;
        this.isLastCall = false;
        this.globalRoutingLog = new Vector();
        this.localRoutingLog = new Vector();
    }
    
    /// Initialises a new instance of the CallInfo class with specified Skill.
    public CallInfo(Skill skill)
    {
        VMID unique = new VMID();
        this.id = unique.toString ();        
        this.skill = skill;
        this.handlerID = "";
        this.callCentres = new Vector();
        
        this.run = 1;
        this.dtArrival = new Date(0);
        this.dtCurrent = new Date(0);
        this.answerTime = 0;
        this.handleTime = 0;
        this.abandonTime = 0;
        
        this.ttl = 0;
        this.isLastCall = false;
        this.globalRoutingLog = new Vector();
        this.localRoutingLog = new Vector();
    }
    
    /// Initialises a new instance of the CallInfo class with id and Skill specified.
    public CallInfo(int intId, Skill skill, Date date)
    {
        this.id = Integer.toString(intId);
        this.skill = skill;
        this.handlerID = "";
        this.callCentres = new Vector();
        
        this.run = 1;
        this.dtArrival = date;
        this.dtCurrent = new Date(0);
        this.answerTime = 0;
        this.handleTime = 0;
        this.abandonTime = 0;
        
        this.ttl = 0;
        this.isLastCall = false;
        this.globalRoutingLog = new Vector();
        this.localRoutingLog = new Vector();
    }
    
    public boolean hasVisitedHandler(String handlerId)
    {
        for(int i=0; i<localRoutingLog.size(); i++){
            if (localRoutingLog.get(i).toString().compareTo(handlerId) == 0) return true;
        }
        return false;
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public Skill getSkill()
    {
        return this.skill;
    }
    
    public void setSkill(Skill skill)
    {
        this.skill = skill;
    }
    
    public void setRun(int run)
    {
        this.run = run;
    }
    
    public int getRun()
    {
        return this.run;
    }
    
    public void setArrival(Date dt)
    {
        this.dtArrival = dt;
    }
    
    public Date getArrival()
    {
        return this.dtArrival;
    }
    
    public void setCurrentTime(Date dt)
    {
        this.dtCurrent = dt;
    }
    
    public Date getCurrentTime()
    {
        return this.dtCurrent;
    }
    
    public void setAnswerTime(double secs)
    {
        this.answerTime = secs;
    }
    
    public double getAnswerTime()
    {
        return this.answerTime;
    }
    
    public double getHandleTime()
    {
        return this.handleTime;
    }
    
    public void setHandleTime(double secs)
    {
        this.handleTime = secs;
    }
    
    public double getAbandonTime()
    {
        return this.abandonTime;
    }
    
    public void setAbandonTime(double secs)
    {
        this.abandonTime = secs;
    }
    
    public void setAgentHandlerID(String handlerID)
    {
        this.handlerID = handlerID;
    }
    
    public String getAgentHandlerID()
    {
        return this.handlerID;
    }
    
    public void addCallCentre(String cc)
    {
        callCentres.add(cc);
    }
    
    public String getCallCentre(int index)
    {
        return callCentres.get(index).toString();
    }
    
    public int getCallCentreCount()
    {
        return this.callCentres.size();
    }
    
    public boolean callCentreExists(String cc)
    {
        for(int i=0; i<callCentres.size(); i++){
            if (callCentres.get(i).toString().compareTo(cc) == 0) return true;
        }
        return false;
    }
    
    public void setLastCall()
    {
        this.isLastCall = true;
    }
    
    public boolean isLastCall()
    {
        return this.isLastCall;
    }
    
    public boolean canAbandon()
    {
        return (this.ttl == 0);
    }
    
    public void setTTL(int ttl)
    {
        this.ttl = ttl;
    }
    
    public void reduceTTL()
    {
        ttl--;
    }
    
    public void logRoute(String handlerAddress)
    {
        localRoutingLog.add(handlerAddress);
    }
    
    public void resetLocalRoutingLog()
    {
        globalRoutingLog.add(localRoutingLog);
        localRoutingLog.clear();
    }
}
