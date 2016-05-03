package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * ForwardList.java
 *
 * Created on 04/09/2008, 19:53
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;


public class ForwardList 
{
    private Vector abandoned; 
    private Vector availableHandlers;
    private CallInfo currentCall;
    private Date currentTime;    
    
    public ForwardList()
    {
        this.availableHandlers = new Vector();
        this.abandoned = new Vector();
        this.currentCall = null;
        this.currentTime = null;
    }
    
    public void setCurrentTime(Date currentTime)
    {
        this.currentTime = currentTime;
    }
    
    public Date getCurrentTime()
    {
        return this.currentTime;
    }
    
    public void setCurrentCall(CallInfo call)
    {
        this.currentCall = call;
    }
    
    public CallInfo getCurrentCall()
    {
        return this.currentCall;
    }
    
    public void addAvailable(HandlerPacket hp)
    {
        this.availableHandlers.add(hp);
    }
    
    public void sortHandlers()
    {
        Collections.sort(availableHandlers, new HandlerPacketComparator());
    }
    
    public void removeBestHandler()
    {
        availableHandlers.remove(0);
    }
    
    public String getBestHandlerId()
    {
        return getAvailableHandler(0).getHandlerID();
    }
    
    public HandlerPacket getAvailableHandler(int index)
    {
        return (HandlerPacket)this.availableHandlers.get(index);
    }
    
    public int getAvailableCount()
    {
        return this.availableHandlers.size();
    }
    
    public void clearAvailable()
    {
        this.availableHandlers.clear();
    }
    
    public void addAbandoned(CallInfo call)
    {
        this.abandoned.add(call);
    }
    
    public CallInfo getAbandonedCall(int index)
    {
        return (CallInfo)this.abandoned.get(index);
    }
    
    public int getAbandonedCount()
    {
        return this.abandoned.size();
    }
    
    public int findHandlerById(String id)
    {
        for(int i=0; i<availableHandlers.size(); i++){
            HandlerPacket hp2 = (HandlerPacket)availableHandlers.get(i);
            if (hp2.getHandlerID().compareTo(id) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    public int findHandler(HandlerPacket hp1)
    {
        for(int i=0; i<availableHandlers.size(); i++){
            HandlerPacket hp2 = (HandlerPacket)availableHandlers.get(i);
            if (hp2.getHandlerID().compareTo(hp1.getHandlerID()) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    private class HandlerPacketComparator implements Comparator 
    {
        public int compare(Object o1, Object o2) 
        {
            HandlerPacket ht1 = (HandlerPacket)o1;
            HandlerPacket ht2 = (HandlerPacket)o2;
            
            return (int)ht1.getNextAvailable().getTime() - (int)ht2.getNextAvailable().getTime();
        }
    }
}
