package com.ccsim;

/*
 * CallCentre.java
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
 * This class holds all the attributes of a call centre and consists of a collection
 * of resources. These resources can either be teams of handlers or handlers.
 */
public class CallCentre implements Serializable
{
    private final int DAYS_OF_WEEK = 7;
    
    private String id;
    private String name;    
    
    private Vector fteDays;
    private Vector htDays;
    
    private Vector totalHandlers;
    
    /** Initialises a new instance of CallCentre and sets everything to the default values.*/
    public CallCentre ()
    {
        VMID unique = new VMID();
        this.id = unique.toString ();   
        this.name = "";
    
        this.fteDays = new Vector();
        this.htDays = new Vector();
        
        this.totalHandlers = new Vector();
    }    
    
    public CallCentre(String id, String name)
    {
        this.id = id;
        this.name = name;
    
        this.fteDays = new Vector();
        this.htDays = new Vector();
        
        this.totalHandlers = new Vector();
    }
    
    public CallCentre(String name)
    {
        VMID unique = new VMID();
        this.id = unique.toString ();   
        this.name = name;
    
        this.fteDays = new Vector();
        this.htDays = new Vector();
        
        this.totalHandlers = new Vector();
    }    
    
    public void generateHandlers()
    {
        // find the distinct skillgroups
        DayShiftInfo day = (DayShiftInfo)fteDays.get(0);
        PeriodSkillGroupInfo period = (PeriodSkillGroupInfo)day.getAt(0);
        for(int i=0; i<period.getCount(); i++){
            // generate handlers per skillgroup
            Vector tmp = generateHandlersPerSkillGroup(period.getAt(i).getSkillGroup());
            for(int x=0; x<tmp.size(); x++){
                totalHandlers.add(tmp.get(x));
            }
        }
    }
        
    public Vector generateHandlersPerSkillGroup(SkillGroup sg)
    {       
        int maxHeight = 0;
        int prevFTE = 0;
        
        Vector handlers = new Vector();
        for(int di=0; di<fteDays.size(); di++){
            DayShiftInfo dsi = (DayShiftInfo)fteDays.get(di);
            DayHandleInfo dhi = (DayHandleInfo)htDays.get(di);
                    
            for(int x=0; x<dsi.periods.size(); x++){
                PeriodSkillGroupInfo period = (PeriodSkillGroupInfo)dsi.periods.get(x);
                PeriodHandleInfo pHT = (PeriodHandleInfo)dhi.periods.get(x);
                
                Date periodStart = period.getStart();
                Date periodFinish = period.getFinish();
                
                int currentFTE = period.getAt(sg).getFTE();
                if (currentFTE > prevFTE) {
                    if (currentFTE > maxHeight) {
                        // allocate new shifts to the previous FTE and create new handlers for the difference of FTE.
                        int fteDifference = (currentFTE - maxHeight);
                        for(int hi=0; hi<maxHeight; hi++){
                            CallHandler handler = (CallHandler)handlers.get(hi);
                            handler.allocateShift(periodStart, periodFinish, sg);
                            for(int hti=0; hti<pHT.getCount(); hti++){
                                handler.setHandleTime(periodStart, periodFinish, pHT.getAt(hti));
                            }
                        }
                        
                        // create new call handlers if required and add them to the cache.
                        Vector newHandlers = makeMultipleHandlers(fteDifference);
                        for(int i=0; i<newHandlers.size(); i++){
                            CallHandler handler = (CallHandler)newHandlers.get(i);
                            handler.allocateShift(periodStart, periodFinish, sg);
                            for(int hti=0; hti<pHT.getCount(); hti++){
                                handler.setHandleTime(periodStart, periodFinish, pHT.getAt(hti));
                            }
                            handlers.add(handler);
                        }                                                
                        maxHeight = currentFTE;                                            
                    } else {
                        // allocate new shifts to the previous FTE.
                        for(int hi=0; hi<currentFTE; hi++){
                            CallHandler handler = (CallHandler)handlers.get(hi);
                            handler.allocateShift(periodStart, periodFinish, sg);
                            for(int hti=0; hti<pHT.getCount(); hti++){
                                handler.setHandleTime(periodStart, periodFinish, pHT.getAt(hti));
                            }
                        }
                    }
                } else {
                    // allocate new shifts to the previous FTE.
                    for(int hi=0; hi<currentFTE; hi++){
                        CallHandler handler = (CallHandler)handlers.get(hi);
                        handler.allocateShift(periodStart, periodFinish, sg);
                        for(int hti=0; hti<pHT.getCount(); hti++){
                            handler.setHandleTime(periodStart, periodFinish, pHT.getAt(hti));
                        }
                    }
                }
                prevFTE = currentFTE;
            }
        }
        return handlers;
    }

    public boolean skillHandled(Skill skill)
    {
        DayShiftInfo day = (DayShiftInfo)fteDays.get(0);
        PeriodSkillGroupInfo period = (PeriodSkillGroupInfo)day.getAt(0);
        for(int i=0; i<period.getCount(); i++){
            if (period.getAt(i).getSkillGroup().findByName(skill.getName()) >= 0){
                return true;
            }
        }
        return false;
    }
    
    public int getDAYS_OF_WEEK()
    {
        return DAYS_OF_WEEK;
    }
    
    public CallHandler getAt(int index)
    {
        return (CallHandler)this.totalHandlers.get(index);
    }
    
    public int getHandlerCount()
    {
        return this.totalHandlers.size();
    }    
    
    public CallHandler makeHandler()
    {
        CallHandler handler = new CallHandler();
        return handler;
    }
    
    public Vector makeMultipleHandlers(int count)
    {
        Vector tmpHandlers  = new Vector();
        for(int i=0; i<count; i++) {
            tmpHandlers.add(makeHandler());
        }
        return tmpHandlers;
    }
    
    public void addFTEDay(DayShiftInfo day)
    {
        this.fteDays.add(day);
    }
    
    public DayShiftInfo getFTEDayAt(int index)
    {
        return (DayShiftInfo)this.fteDays.get(index);
    }
    
    public void addHTDay(DayHandleInfo day)
    {
        this.htDays.add(day);
    }
    
    public DayHandleInfo getHTDayAt(int index)
    {
        return (DayHandleInfo)this.htDays.get(index);
    }
    
    public int getFTECount()
    {
        return this.fteDays.size();
    }
    
    public int getHTCount()
    {
        return this.htDays.size();
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
}
