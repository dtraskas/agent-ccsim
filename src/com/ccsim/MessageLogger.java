package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * MessageLog.java
 *
 * Created on 29/07/2008,20:33
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */

public class MessageLogger 
{
    private String agentId;
    private int totalReceived;
    private int totalSent;
    private int callsReceived;
    private int callsForwarded;
    
    public MessageLogger(String agentId)
    {
        this.agentId = agentId;
        this.totalReceived = 0;
        this.totalSent = 0;
        this.callsReceived = 0;
        this.callsForwarded = 0;
    }
    
    public String getAgentId()
    {
        return this.agentId;
    }
    
    public void AddReceived()
    {
        this.totalReceived++;
    }
    
    public void AddSent()
    {
        this.totalSent++;
    }
    
    public void AddReceivedCall()
    {
        this.callsReceived++;
        AddReceived();
    }
    
    public void AddForwardedCall()
    {
        this.callsForwarded++;
        AddSent();
    }
    
    public int getTotalReceived()
    {
        return this.totalReceived;
    }
    
    public int getTotalSent()
    {
        return this.totalSent;
    }
    
    public int getCallsReceived()
    {
        return this.callsReceived;
    }
    
    public int getCallsForwarded()
    {
        return this.callsForwarded;
    }
}
