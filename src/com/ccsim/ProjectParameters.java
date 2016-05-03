package com.ccsim;

/*
 * ProjectParameters.java
 *
 * Created on February 5, 2008, 9:33 PM
 * Bath University
 *
 */
import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

/**
 *
 * @author dimitriostraskas
 */
public class ProjectParameters implements Serializable
{
    private Date start;
    private Date finish;
    private int interval;
    private int totalRuns;
    private Vector groups;
    
    /** Creates a new instance of ProjectParameters */
    public ProjectParameters()
    {
        this.start = new Date(0);
        this.finish = new Date(0);
        this.interval = 0;
        this.totalRuns = 1;
        this.groups = new Vector();
    }
    
    public ProjectParameters(Date start, Date finish, int interval)
    {
        this.start = start;
        this.finish = finish;
        this.interval = interval;
        this.totalRuns = 1;
        this.groups = new Vector();
    }
    
    public void setStart(Date start)
    {
        this.start = start;
    }
    
    public Date getStart()
    {
        return this.start;
    }
    
    public void setFinish(Date finish)
    {
        this.finish = finish;
    }
    
    public Date getFinish()
    {
        return this.finish;
    }
    
    public void setInterval(int interval)
    {
        this.interval = interval;
    }
    
    public int getInterval()
    {
        return this.interval;
    }
    
    public int getTotalRuns()
    {
        return this.totalRuns;
    }
    
    public void setTotalRuns(int runs)
    {
        this.totalRuns = runs;
    }
    
    public void addGroup(CallCentre group)
    {
        this.groups.add(group);
    }
    
    public Vector getGroups()
    {
        return this.groups;
    }
}
