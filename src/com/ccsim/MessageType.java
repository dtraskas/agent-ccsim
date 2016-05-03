package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * MessageType.java
 *
 * Created on 29/08/2008, 20:50
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */

public enum MessageType
{
    DataRequest,
    NextCallRequest,
    LogCall,
    LogMessageCount,
    Registered,
    CallForward,
    CallAbandon,
    CallAllocate,
    CallRemove,
    Ping,
    Pong,
    BootstrapComplete
};
