package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;
import java.rmi.dgc.*;

/**
 * InfoPacket.java
 *
* Created on 18 Jan 2008, 23:43
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */
public class InfoPacket implements Serializable
{
    private String id;
    private String subject;
    private Object content;
    
    public InfoPacket()
    {
        VMID unique = new VMID();
        this.id = unique.toString ();
        this.subject = "";
        this.content = null;
    }
    
    public InfoPacket(String subject, Object content)
    {
        this.subject = subject;
        this.content = content;
    }
    
    public void setSubject(String subject)
    {
        this.subject = subject;
    }
    
    public void setContent(Object content)
    {
        this.content = content;
    }
    
    public String getSubject()
    {
        return this.subject;
    }
    
    public Object getContent()
    {
        return this.content;
    }
}
