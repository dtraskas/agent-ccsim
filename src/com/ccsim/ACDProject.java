package com.ccsim;

/*
 * ACDProject.java
 *
 * Created on January 7, 2008, 10:09 PM
 * Bath University
 *
 */

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author dimitriostraskas
 */
public class ACDProject
{
    private String path;
    public final int DEFAULT_INTERVAL = 30;
    
    private Vector skills;
    private Vector skillgroups;
    private DayCallInfo callsDay;
    private DayAbandonInfo abandonDay;
    private Vector resources;    
    private Vector globalQueue;    
    
    private int totalPeriods;
    private int totalCalls;
    private int totalHandlers;
        
    private DateFormat df = new SimpleDateFormat("HH:mm:ss");
    
    /** Creates a new instance of DataLoader */
    public ACDProject() {}
    
    public AppUtils.State load(String path)
    {
        this.path = path;
        this.skills = new Vector();
        this.skillgroups = new Vector();
        this.callsDay = new DayCallInfo();
        this.abandonDay = new DayAbandonInfo();
        this.resources = new Vector();
        this.globalQueue = new Vector();
        
        this.totalPeriods = 0;
        this.totalCalls = 0;
        this.totalHandlers = 0;
        
        AppUtils.State rc = AppUtils.State.FAILURE;        
        
        rc = loadResources();
        if (rc == AppUtils.State.SUCCESS) rc = loadSkills();
        if (rc == AppUtils.State.SUCCESS) rc = loadSkillGroups();
        if (rc == AppUtils.State.SUCCESS) rc = loadCalls();
        if (rc == AppUtils.State.SUCCESS) rc = loadAbandonment();
        if (rc == AppUtils.State.SUCCESS) rc = loadFTE();
        if (rc == AppUtils.State.SUCCESS) rc = loadHT();
        
        return rc;
    }
    
    public void unload()
    {
        this.path = "";
        this.skills = new Vector();
        this.skillgroups = new Vector();
        this.callsDay = new DayCallInfo();
        this.abandonDay = new DayAbandonInfo();
        this.resources = new Vector();
        this.globalQueue = new Vector();
        
        this.totalPeriods = 0;
        this.totalCalls = 0;
        this.totalHandlers = 0;
    }
    
    public void generate()
    {
        generateCalls();
        generateAgents();
        for(Enumeration e = getCallCentres().elements(); e.hasMoreElements ();){
            CallCentre cc = (CallCentre)e.nextElement();
            totalHandlers += cc.getHandlerCount();
        }               
    }

    public void generatePerSkill()
    {
        generateCalls();
        generateAgents();
        for(Enumeration e = resources.elements(); e.hasMoreElements ();){
            CallCentre cc = (CallCentre)e.nextElement();
            for(int i=0; i<cc.getHandlerCount(); i++){
                CallHandler ch = (CallHandler)cc.getAt(i);
                for(int s=0; s<ch.getSkillCount(); s++){
                    totalHandlers ++; 
                }
            }
        }
    }

    
    public int getTotalHandlers()
    {
        return this.totalHandlers;
    }
    
    public Vector getGlobalQueue()
    {
        return this.globalQueue;
    }
    
    public int getPeriods()
    {
        return this.totalPeriods;
    }
    
    public int getTotalCalls()
    {
        return this.totalCalls;
    }
    
    public ProjectParameters getParams()
    {
        ProjectParameters params = new ProjectParameters(getSimStart(), getSimFinish(), DEFAULT_INTERVAL);
        for(int i=0; i<resources.size(); i++){
            params.addGroup((CallCentre)resources.get(i));
        }
        
        return params;
    }
    
    public int getDefaultInterval()
    {
        return this.DEFAULT_INTERVAL;
    }
    
    public String getSimStartString()
    {
        return df.format(callsDay.getAt(0).getStart());
    }
    
    public String getSimFinishString()
    {
        return df.format(callsDay.getAt(callsDay.getCount() - 1).getFinish());
    }
    
    public Date getSimStart()
    {
        return callsDay.getAt(0).getStart();
    }
    
    public Date getSimFinish()
    {
        return callsDay.getAt(callsDay.getCount() - 1).getFinish();
    }
    
    public Vector getSkills()
    {
        return this.skills;
    }
    
    public Vector getSkillGroups()
    {
        return this.skillgroups;
    }
    
    public DayCallInfo getWorkload()
    {
        return this.callsDay;
    }
    
    public DayAbandonInfo getAbandonment()
    {
        return this.abandonDay;
    }
    
    public void generateAgents()
    {
        for(Enumeration e = resources.elements (); e.hasMoreElements ();){
            CallCentre cc = (CallCentre)e.nextElement();
            cc.generateHandlers();
        }
    }
    
    public void generateCalls()
    {
        Random rnd = new Random();
        Calendar cal = new GregorianCalendar();
        int secsPerInterval = getDefaultInterval() * 60;
        
        DayCallInfo calls = getWorkload();
        DayAbandonInfo abandonment = getAbandonment();
        
        this.globalQueue = new Vector();
        int id = 0;
        for (int x=0; x<calls.getCount(); x++){
            PeriodCallInfo pci = (PeriodCallInfo)calls.getAt(x);
            PeriodAbandonInfo pai = (PeriodAbandonInfo)abandonment.getAt(x);
            
            for(int i=0; i<pci.getCount(); i++){
                int volume = pci.getAt(i).getVolume();
                Skill skill = pci.getAt(i).getSkill();
                int index = pai.find(skill);
                DistributionExponential dist = (DistributionExponential)pai.getAt(index).getAbandonTime();
                        
                for(int j=0; j<volume; j++){            
                    int secs = rnd.nextInt(secsPerInterval);
                    
                    cal.setTime(pci.getStart());
                    cal.add(Calendar.SECOND, secs);
                    
                    CallInfo ci = new CallInfo(id++, skill, cal.getTime());
                    ci.setAbandonTime(dist.getRandom());
                    
                    for(int rs=0; rs<resources.size(); rs++){
                        CallCentre centre = (CallCentre)resources.get(rs);
                        if (centre.skillHandled(ci.getSkill())){
                            ci.addCallCentre(centre.getName()); 
                        }
                    }                    
                    this.globalQueue.add(ci);                    
                }
            }
        }
        Collections.sort(globalQueue, new CallComparator());
        this.totalCalls = globalQueue.size();
    }
    
    public Vector getCallCentres()
    {
        return this.resources;
    }
    
    public AppUtils.State loadResources()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/RESOURCES.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            
            // read the headers
            String line = buffer.readLine();
            //read each line of text file
            while((line = buffer.readLine()) != null) {	
                String[] result = line.split(",");
                
                String name = result[0];
                CallCentre centre = new CallCentre(name);                
                this.resources.add(centre);
            }
            buffer.close();
            
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadSkills()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/SKILLS.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            
            // read the headers
            String line = buffer.readLine();
            //read each line of text file
            while((line = buffer.readLine()) != null) {	
                String[] result = line.split(",");
                
                String name = result[0];
                int priority = Integer.parseInt(result[1]);
                int tsf = Integer.parseInt(result[2]);
                
                Skill skill = new Skill(name);
                skill.setPriority(priority);
                skill.setTsf(tsf);
                
                this.skills.add(skill);
            }
            buffer.close();
            
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadSkillGroups()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/SGS.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));

            String line = buffer.readLine();
            String[] result = line.split(",");
            // read the first header and then create all the skillgroups
            for(int i=1; i<result.length; i++){
                SkillGroup sg = new SkillGroup(result[i]);
                this.skillgroups.add(sg);
            }

            //read each line of text file
            while((line = buffer.readLine()) != null) {	
                // now add the skills to the skillgroups.
                int cnt = 0;
                // get the skill first
                result = line.split(",");
                String skillName = result[0];
                int index = findSkill(skillName);
                Skill skill = (Skill)skills.get(index);
                
                for(int i=1; i<result.length; i++){
                    int added = Integer.parseInt(result[i]);
                    if (added == 1) {
                        ((SkillGroup)this.skillgroups.get(cnt)).addSkill(skill);
                    }
                    cnt++;
                }
            }
            buffer.close();
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadCalls()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/CALLS.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));

            // read the headers of the calls file.
            DateFormat df = new SimpleDateFormat("HH:mm");       
            Calendar cal = new GregorianCalendar();
            
            String line = buffer.readLine();
            String[] result = line.split(",");
            
            // build the day periods.
            for(int i=1; i<result.length; i++){
                Date start =  df.parse(result[i]);
                cal.setTime(start);                
                cal.add(Calendar.MINUTE, DEFAULT_INTERVAL);
                Date finish = cal.getTime();
                
                PeriodCallInfo period = new PeriodCallInfo();
                period.setStart(start);
                period.setFinish(finish);
                callsDay.add(period);
            }
            
            //read each skill and period.
            while((line = buffer.readLine()) != null) {	
                result = line.split(",");
                int index = findSkill(result[0]);
                Skill skill = (Skill)skills.get(index);
                
                int idx = 0;
                for(int i=1; i<result.length; i++){
                    int volume = Integer.parseInt(result[i]);
                    ((PeriodCallInfo)callsDay.getAt(idx++)).add(new CallVolume(skill, volume));
                }                
            }
            buffer.close();
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadAbandonment()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/ABANDON.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));

            // read the headers of the calls file.
            DateFormat df = new SimpleDateFormat("HH:mm");       
            Calendar cal = new GregorianCalendar();
            
            String line = buffer.readLine();
            String[] result = line.split(",");
            
            // build the day periods.
            for(int i=1; i<result.length; i++){
                Date start =  df.parse(result[i]);
                cal.setTime(start);                
                cal.add(Calendar.MINUTE, DEFAULT_INTERVAL);
                Date finish = cal.getTime();
                
                PeriodAbandonInfo period = new PeriodAbandonInfo();
                period.setStart(start);
                period.setFinish(finish);
                abandonDay.add(period);
            }
            
            //read each skill and period.
            while((line = buffer.readLine()) != null) {	
                result = line.split(",");
                int index = findSkill(result[0]);
                Skill skill = (Skill)skills.get(index);
                
                int idx = 0;
                for(int i=1; i<result.length; i++){
                    double mean = Double.parseDouble(result[i]);
                    DistributionExponential dist = new DistributionExponential(mean);
                    ((PeriodAbandonInfo)abandonDay.getAt(idx++)).add(new AbandonInfo(skill, dist));
                }                
            }
            buffer.close();
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadFTE()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/FTE.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));

            // read the headers of the calls file.
            DateFormat df = new SimpleDateFormat("HH:mm");       
            Calendar cal = new GregorianCalendar();
            
            String line = buffer.readLine();
            String[] result = line.split(",");
            
            // build the day periods.
            DayShiftInfo fteDay = new DayShiftInfo();
            for(int i=2; i<result.length; i++){
                Date start =  df.parse(result[i]);
                cal.setTime(start);                
                cal.add(Calendar.MINUTE, DEFAULT_INTERVAL);
                Date finish = cal.getTime();
                
                PeriodSkillGroupInfo period = new PeriodSkillGroupInfo();
                period.setStart(start);
                period.setFinish(finish);
                fteDay.add(period);
            }
            
            //read each skill and period.
            while((line = buffer.readLine()) != null) {	
                result = line.split(",");
                int index = findResource(result[0]);
                CallCentre cc = (CallCentre)resources.get(index);
                
                DayShiftInfo day = null;
                if (cc.getFTECount() == 0){
                    day = new DayShiftInfo();
                    cc.addFTEDay(day);

                    for(int i=0; i<fteDay.getCount(); i++){
                        PeriodSkillGroupInfo p = (PeriodSkillGroupInfo)fteDay.getAt(i);
                        PeriodSkillGroupInfo period = new PeriodSkillGroupInfo();
                        period.setStart(p.getStart());
                        period.setFinish(p.getFinish());
                        day.add(period);                        
                    }
                    totalPeriods = fteDay.getCount();
                } else {
                    day = cc.getFTEDayAt(0);
                }
                
                index = findSkillGroup(result[1]);
                SkillGroup sg = (SkillGroup)skillgroups.get(index);
                
                int idx = 0;
                for(int i=2; i<result.length; i++){
                    int fte = Integer.parseInt(result[i]);
                    
                    PeriodSkillGroupInfo period = ((PeriodSkillGroupInfo)day.getAt(idx++));
                    period.add(new SkillGroupInfo(sg, fte));
                }                
            }
            buffer.close();
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public AppUtils.State loadHT()
    {
        AppUtils.State rc = AppUtils.State.FAILURE;
        try{        
            File file = new File(path + "/HT.csv");
            BufferedReader buffer = new BufferedReader(new FileReader(file));

            // read the headers of the calls file.
            DateFormat df = new SimpleDateFormat("HH:mm");       
            Calendar cal = new GregorianCalendar();
            
            String line = buffer.readLine();
            String[] result = line.split(",");
            
            // build the day periods.
            DayHandleInfo htDay = new DayHandleInfo();
            for(int i=2; i<result.length; i++){
                Date start =  df.parse(result[i]);
                cal.setTime(start);                
                cal.add(Calendar.MINUTE, DEFAULT_INTERVAL);
                Date finish = cal.getTime();
                
                PeriodHandleInfo period = new PeriodHandleInfo();
                period.setStart(start);
                period.setFinish(finish);
                htDay.add(period);
            }
            
            //read each skill and period.
            while((line = buffer.readLine()) != null) {	
                result = line.split(",");
                
                int index = findResource(result[0]);
                CallCentre cc = (CallCentre)resources.get(index);
                
                DayHandleInfo day = null;
                if (cc.getHTCount() == 0) {
                    day = new DayHandleInfo();
                    cc.addHTDay(day);
                    
                    for(int i=0; i<htDay.getCount(); i++){
                        PeriodHandleInfo p = (PeriodHandleInfo)htDay.getAt(i);
                        PeriodHandleInfo period = new PeriodHandleInfo();
                        period.setStart(p.getStart());
                        period.setFinish(p.getFinish());
                        day.add(period);
                    }
                } else {
                    day = cc.getHTDayAt(0);
                }
                
                index = findSkill(result[1]);
                Skill skill = (Skill)skills.get(index);
                
                int idx = 0;
                for(int i=2; i<result.length; i++){
                    double mean = Double.parseDouble(result[i]);
                    DistributionExponential dist = new DistributionExponential(mean);
                    
                    PeriodHandleInfo period = ((PeriodHandleInfo)day.getAt(idx++));
                    period.add(new HandleInfo(skill, dist));
                }                
            }
            buffer.close();
            rc = AppUtils.State.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rc;
    }
    
    public int findSkill(String name)
    {
        for(int i=0; i<skills.size(); i++){
            if (((Skill)skills.get(i)).getName().compareTo(name) == 0) {
                return i;
            }
        }
        return -1;
    }

    public int findSkillGroup(String name)
    {
        for(int i=0; i<skillgroups.size(); i++){
            if (((SkillGroup)skillgroups.get(i)).getName().compareTo(name) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    public int findResource(String name)
    {
        for(int i=0; i<resources.size(); i++){
            if (((CallCentre)resources.get(i)).getName().compareTo(name) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    public String getPath()
    {
        return path;
    }
    
    private class CallComparator implements Comparator 
    {
        public int compare(Object o1, Object o2) 
        {
            CallInfo ci1 = (CallInfo)o1;
            CallInfo ci2 = (CallInfo)o2;
            return (int)ci1.getArrival().getTime() - (int)ci2.getArrival().getTime();     
        }
    }
    
    //<editor-fold desc="Debugging Functions">
    
    public void debugGenerateCalls()
    {
        Calendar cal = new GregorianCalendar();
        
        DayCallInfo calls = getWorkload();
        DayAbandonInfo abandonment = getAbandonment();
        
        this.globalQueue = new Vector();
        int id = 0;
        Vector strings = new Vector();
        strings.add("08:01:34");
        strings.add("08:02:09");
        strings.add("08:02:49");
        strings.add("08:02:53");
        strings.add("08:04:30");
        strings.add("08:05:16");
        strings.add("08:05:35");
        strings.add("08:07:33");
        strings.add("08:08:00");
        strings.add("08:08:49");
        strings.add("08:10:21");
        strings.add("08:11:42");
        strings.add("08:11:59");
        strings.add("08:12:23");
        strings.add("08:13:29");
        strings.add("08:16:09");
        strings.add("08:19:31");
        strings.add("08:20:01");
        strings.add("08:20:03");
        strings.add("08:21:25");
        strings.add("08:22:28");
        strings.add("08:24:18");
        strings.add("08:24:25");
        strings.add("08:25:19");
        strings.add("08:25:29");
        strings.add("08:27:19");
        strings.add("08:27:24");
        strings.add("08:29:00");
        strings.add("08:29:10");
        strings.add("08:29:19");
        strings.add("08:30:49");
        strings.add("08:30:50");
        strings.add("08:32:09");
        strings.add("08:35:28");
        strings.add("08:37:05");
        strings.add("08:38:03");
        strings.add("08:39:39");
        strings.add("08:41:31");
        strings.add("08:42:48");
        strings.add("08:44:47");
        strings.add("08:45:56");
        strings.add("08:46:02");
        strings.add("08:46:33");
        strings.add("08:46:54");
        strings.add("08:48:17");
        strings.add("08:49:09");
        strings.add("08:49:37");
        strings.add("08:51:40");
        strings.add("08:52:07");
        strings.add("08:54:42");
        strings.add("08:55:05");
        strings.add("08:56:07");
        strings.add("08:56:12");
        strings.add("08:56:15");
        strings.add("08:56:53");
        strings.add("08:56:55");
        strings.add("08:57:18");
        strings.add("08:57:29");
        strings.add("08:57:45");
        strings.add("08:58:33");
        strings.add("09:03:35");
        strings.add("09:03:40");
        strings.add("09:04:32");
        strings.add("09:05:17");
        strings.add("09:05:50");
        strings.add("09:05:57");
        strings.add("09:07:11");
        strings.add("09:08:06");
        strings.add("09:08:12");
        strings.add("09:09:52");
        strings.add("09:09:53");
        strings.add("09:11:13");
        strings.add("09:11:14");
        strings.add("09:11:28");
        strings.add("09:11:35");
        strings.add("09:14:57");
        strings.add("09:16:19");
        strings.add("09:17:22");
        strings.add("09:18:00");
        strings.add("09:18:10");
        strings.add("09:18:26");
        strings.add("09:19:56");
        strings.add("09:20:00");
        strings.add("09:24:31");
        strings.add("09:25:19");
        strings.add("09:25:45");
        strings.add("09:26:32");
        strings.add("09:26:43");
        strings.add("09:29:20");
        strings.add("09:29:24");
        strings.add("09:31:16");
        strings.add("09:31:58");
        strings.add("09:32:28");
        strings.add("09:32:45");
        strings.add("09:33:02");
        strings.add("09:35:26");
        strings.add("09:36:03");
        strings.add("09:36:34");
        strings.add("09:36:46");
        strings.add("09:37:01");
        strings.add("09:37:01");
        strings.add("09:38:15");
        strings.add("09:38:39");
        strings.add("09:39:02");
        strings.add("09:39:52");
        strings.add("09:40:16");
        strings.add("09:43:16");
        strings.add("09:43:18");
        strings.add("09:43:43");
        strings.add("09:44:14");
        strings.add("09:50:43");
        strings.add("09:50:43");
        strings.add("09:51:03");
        strings.add("09:53:06");
        strings.add("09:53:10");
        strings.add("09:53:31");
        strings.add("09:55:19");
        strings.add("09:56:15");
        strings.add("09:56:21");
        strings.add("09:58:11");
        strings.add("10:00:55");
        strings.add("10:01:12");
        strings.add("10:01:19");
        strings.add("10:02:48");
        strings.add("10:03:17");
        strings.add("10:05:11");
        strings.add("10:09:45");
        strings.add("10:12:44");
        strings.add("10:13:27");
        strings.add("10:14:31");
        strings.add("10:15:27");
        strings.add("10:15:42");
        strings.add("10:16:15");
        strings.add("10:16:47");
        strings.add("10:19:19");
        strings.add("10:21:17");
        strings.add("10:21:21");
        strings.add("10:22:04");
        strings.add("10:22:12");
        strings.add("10:22:49");
        strings.add("10:23:13");
        strings.add("10:23:31");
        strings.add("10:24:17");
        strings.add("10:24:39");
        strings.add("10:25:20");
        strings.add("10:25:39");
        strings.add("10:25:55");
        strings.add("10:25:56");
        strings.add("10:27:37");
        strings.add("10:29:36");
        strings.add("10:30:28");
        strings.add("10:30:30");
        strings.add("10:34:56");
        strings.add("10:37:02");
        strings.add("10:37:24");
        strings.add("10:37:24");
        strings.add("10:38:27");
        strings.add("10:38:44");
        strings.add("10:39:04");
        strings.add("10:40:22");
        strings.add("10:40:47");
        strings.add("10:41:18");
        strings.add("10:43:36");
        strings.add("10:47:45");
        strings.add("10:48:33");
        strings.add("10:49:11");
        strings.add("10:49:42");
        strings.add("10:49:49");
        strings.add("10:49:50");
        strings.add("10:50:17");
        strings.add("10:51:39");
        strings.add("10:51:45");
        strings.add("10:53:53");
        strings.add("10:54:14");
        strings.add("10:54:29");
        strings.add("10:56:05");
        strings.add("10:56:59");
        strings.add("10:57:58");
        strings.add("10:59:06");
        strings.add("10:59:46");
        strings.add("11:02:27");
        strings.add("11:02:54");
        strings.add("11:03:33");
        strings.add("11:03:42");
        strings.add("11:04:12");
        strings.add("11:04:14");
        strings.add("11:06:50");
        strings.add("11:07:28");
        strings.add("11:08:08");
        strings.add("11:08:35");
        strings.add("11:09:46");
        strings.add("11:10:51");
        strings.add("11:12:17");
        strings.add("11:12:35");
        strings.add("11:14:49");
        strings.add("11:16:50");
        strings.add("11:17:04");
        strings.add("11:18:43");
        strings.add("11:19:17");
        strings.add("11:19:18");
        strings.add("11:20:31");
        strings.add("11:21:01");
        strings.add("11:21:39");
        strings.add("11:22:11");
        strings.add("11:23:23");
        strings.add("11:24:11");
        strings.add("11:25:10");
        strings.add("11:25:22");
        strings.add("11:26:13");
        strings.add("11:29:45");
        strings.add("11:30:36");
        strings.add("11:31:33");
        strings.add("11:32:21");
        strings.add("11:32:22");
        strings.add("11:32:27");
        strings.add("11:33:48");
        strings.add("11:35:58");
        strings.add("11:38:51");
        strings.add("11:38:51");
        strings.add("11:39:23");
        strings.add("11:39:47");
        strings.add("11:41:38");
        strings.add("11:41:45");
        strings.add("11:42:02");
        strings.add("11:42:10");
        strings.add("11:42:11");
        strings.add("11:42:59");
        strings.add("11:43:37");
        strings.add("11:43:58");
        strings.add("11:45:03");
        strings.add("11:45:31");
        strings.add("11:45:49");
        strings.add("11:48:58");
        strings.add("11:49:37");
        strings.add("11:50:21");
        strings.add("11:53:44");
        strings.add("11:54:07");
        strings.add("11:55:17");
        strings.add("11:58:50");
        strings.add("11:59:38");
        strings.add("12:00:20");
        strings.add("12:00:45");
        strings.add("12:02:50");
        strings.add("12:04:45");
        strings.add("12:04:53");
        strings.add("12:06:54");
        strings.add("12:07:24");
        strings.add("12:08:07");
        strings.add("12:09:01");
        strings.add("12:10:42");
        strings.add("12:11:33");
        strings.add("12:11:39");
        strings.add("12:13:14");
        strings.add("12:14:49");
        strings.add("12:15:22");
        strings.add("12:16:35");
        strings.add("12:18:01");
        strings.add("12:18:31");
        strings.add("12:18:41");
        strings.add("12:21:19");
        strings.add("12:22:02");
        strings.add("12:23:51");
        strings.add("12:24:17");
        strings.add("12:24:40");
        strings.add("12:24:52");
        strings.add("12:25:12");
        strings.add("12:28:02");
        strings.add("12:28:18");
        strings.add("12:28:49");
        strings.add("12:28:52");
        strings.add("12:30:26");
        strings.add("12:34:14");
        strings.add("12:34:44");
        strings.add("12:35:02");
        strings.add("12:36:36");
        strings.add("12:36:43");
        strings.add("12:37:04");
        strings.add("12:37:42");
        strings.add("12:39:06");
        strings.add("12:39:54");
        strings.add("12:40:02");
        strings.add("12:40:30");
        strings.add("12:40:48");
        strings.add("12:41:04");
        strings.add("12:41:40");
        strings.add("12:42:19");
        strings.add("12:42:27");
        strings.add("12:42:45");
        strings.add("12:43:00");
        strings.add("12:43:38");
        strings.add("12:45:17");
        strings.add("12:45:52");
        strings.add("12:46:08");
        strings.add("12:46:15");
        strings.add("12:46:39");
        strings.add("12:48:14");
        strings.add("12:50:28");
        strings.add("12:50:43");
        strings.add("12:52:36");
        strings.add("12:54:45");
        strings.add("12:54:51");
        strings.add("12:55:24");
        strings.add("12:56:09");
        strings.add("12:56:10");
        strings.add("12:56:28");
        strings.add("12:56:29");
        strings.add("12:56:54");
        strings.add("12:57:35");
        strings.add("12:59:07");
        strings.add("12:59:16");
        strings.add("13:01:02");
        strings.add("13:01:26");
        strings.add("13:01:55");
        strings.add("13:02:07");
        strings.add("13:02:19");
        strings.add("13:03:23");
        strings.add("13:03:46");
        strings.add("13:03:59");
        strings.add("13:05:10");
        strings.add("13:05:56");
        strings.add("13:07:39");
        strings.add("13:08:08");
        strings.add("13:08:25");
        strings.add("13:09:04");
        strings.add("13:10:14");
        strings.add("13:10:16");
        strings.add("13:10:25");
        strings.add("13:10:35");
        strings.add("13:11:40");
        strings.add("13:12:08");
        strings.add("13:12:39");
        strings.add("13:14:02");
        strings.add("13:14:21");
        strings.add("13:15:14");
        strings.add("13:16:22");
        strings.add("13:17:39");
        strings.add("13:18:50");
        strings.add("13:19:07");
        strings.add("13:19:18");
        strings.add("13:20:26");
        strings.add("13:20:41");
        strings.add("13:21:39");
        strings.add("13:22:26");
        strings.add("13:23:39");
        strings.add("13:25:27");
        strings.add("13:26:14");
        strings.add("13:26:43");
        strings.add("13:27:37");
        strings.add("13:28:11");
        strings.add("13:29:53");
        strings.add("13:31:22");
        strings.add("13:31:25");
        strings.add("13:31:38");
        strings.add("13:32:19");
        strings.add("13:32:21");
        strings.add("13:33:27");
        strings.add("13:33:42");
        strings.add("13:34:45");
        strings.add("13:36:42");
        strings.add("13:39:35");
        strings.add("13:39:44");
        strings.add("13:39:47");
        strings.add("13:40:37");
        strings.add("13:41:53");
        strings.add("13:42:16");
        strings.add("13:42:35");
        strings.add("13:44:12");
        strings.add("13:45:25");
        strings.add("13:45:27");
        strings.add("13:45:55");
        strings.add("13:46:01");
        strings.add("13:46:14");
        strings.add("13:47:07");
        strings.add("13:49:13");
        strings.add("13:49:17");
        strings.add("13:49:21");
        strings.add("13:50:56");
        strings.add("13:51:30");
        strings.add("13:53:53");
        strings.add("13:54:30");
        strings.add("13:55:18");
        strings.add("13:56:18");
        strings.add("13:56:44");
        strings.add("13:57:13");
        strings.add("13:57:28");
        strings.add("13:57:38");
        strings.add("13:58:02");
        strings.add("13:58:11");
        strings.add("13:58:19");
        strings.add("13:58:58");
        strings.add("14:00:07");
        strings.add("14:00:57");
        strings.add("14:01:26");
        strings.add("14:02:39");
        strings.add("14:02:43");
        strings.add("14:03:45");
        strings.add("14:04:03");
        strings.add("14:04:11");
        strings.add("14:04:50");
        strings.add("14:05:00");
        strings.add("14:06:01");
        strings.add("14:06:05");
        strings.add("14:06:08");
        strings.add("14:08:25");
        strings.add("14:09:00");
        strings.add("14:09:51");
        strings.add("14:11:25");
        strings.add("14:11:31");
        strings.add("14:12:46");
        strings.add("14:14:45");
        strings.add("14:14:45");
        strings.add("14:14:54");
        strings.add("14:16:36");
        strings.add("14:17:40");
        strings.add("14:17:56");
        strings.add("14:19:54");
        strings.add("14:20:46");
        strings.add("14:22:13");
        strings.add("14:22:19");
        strings.add("14:22:55");
        strings.add("14:23:10");
        strings.add("14:24:24");
        strings.add("14:24:40");
        strings.add("14:25:00");
        strings.add("14:25:01");
        strings.add("14:26:01");
        strings.add("14:26:04");
        strings.add("14:27:01");
        strings.add("14:27:16");
        strings.add("14:29:27");
        strings.add("14:32:11");
        strings.add("14:32:15");
        strings.add("14:33:55");
        strings.add("14:33:58");
        strings.add("14:34:28");
        strings.add("14:34:35");
        strings.add("14:35:09");
        strings.add("14:35:13");
        strings.add("14:35:28");
        strings.add("14:36:03");
        strings.add("14:38:44");
        strings.add("14:40:14");
        strings.add("14:40:20");
        strings.add("14:40:39");
        strings.add("14:41:57");
        strings.add("14:42:44");
        strings.add("14:43:02");
        strings.add("14:44:58");
        strings.add("14:47:45");
        strings.add("14:48:23");
        strings.add("14:48:25");
        strings.add("14:50:49");
        strings.add("14:51:49");
        strings.add("14:51:57");
        strings.add("14:52:51");
        strings.add("14:54:18");
        strings.add("14:54:45");
        strings.add("14:55:23");
        strings.add("14:57:51");
        strings.add("14:59:57");
        strings.add("15:01:17");
        strings.add("15:02:17");
        strings.add("15:02:35");
        strings.add("15:03:42");
        strings.add("15:04:46");
        strings.add("15:07:25");
        strings.add("15:07:53");
        strings.add("15:08:49");
        strings.add("15:10:23");
        strings.add("15:11:10");
        strings.add("15:12:10");
        strings.add("15:12:30");
        strings.add("15:12:39");
        strings.add("15:13:10");
        strings.add("15:14:39");
        strings.add("15:14:40");
        strings.add("15:16:38");
        strings.add("15:16:38");
        strings.add("15:17:52");
        strings.add("15:21:05");
        strings.add("15:21:57");
        strings.add("15:22:16");
        strings.add("15:22:32");
        strings.add("15:22:38");
        strings.add("15:24:02");
        strings.add("15:24:21");
        strings.add("15:25:02");
        strings.add("15:26:40");
        strings.add("15:28:46");
        strings.add("15:29:59");
        strings.add("15:31:00");
        strings.add("15:31:09");
        strings.add("15:31:34");
        strings.add("15:33:45");
        strings.add("15:35:41");
        strings.add("15:35:54");
        strings.add("15:36:37");
        strings.add("15:38:56");
        strings.add("15:39:43");
        strings.add("15:39:58");
        strings.add("15:40:50");
        strings.add("15:41:35");
        strings.add("15:42:34");
        strings.add("15:45:37");
        strings.add("15:47:46");
        strings.add("15:48:27");
        strings.add("15:49:49");
        strings.add("15:51:50");
        strings.add("15:52:06");
        strings.add("15:53:06");
        strings.add("15:53:17");
        strings.add("15:53:18");
        strings.add("15:53:30");
        strings.add("15:53:38");
        strings.add("15:53:47");
        strings.add("15:55:26");
        strings.add("15:55:41");
        strings.add("15:57:15");
        strings.add("15:58:41");
        strings.add("15:59:24");
        strings.add("16:00:41");
        strings.add("16:01:11");
        strings.add("16:01:34");
        strings.add("16:01:40");
        strings.add("16:03:41");
        strings.add("16:04:52");
        strings.add("16:05:02");
        strings.add("16:05:54");
        strings.add("16:10:14");
        strings.add("16:12:28");
        strings.add("16:12:56");
        strings.add("16:12:59");
        strings.add("16:12:59");
        strings.add("16:13:58");
        strings.add("16:16:07");
        strings.add("16:16:46");
        strings.add("16:17:35");
        strings.add("16:19:01");
        strings.add("16:19:18");
        strings.add("16:19:23");
        strings.add("16:19:32");
        strings.add("16:20:04");
        strings.add("16:21:35");
        strings.add("16:21:57");
        strings.add("16:23:35");
        strings.add("16:24:17");
        strings.add("16:25:10");
        strings.add("16:27:25");
        strings.add("16:27:30");
        strings.add("16:28:16");
        strings.add("16:32:25");
        strings.add("16:32:32");
        strings.add("16:33:47");
        strings.add("16:34:04");
        strings.add("16:34:05");
        strings.add("16:35:16");
        strings.add("16:35:43");
        strings.add("16:36:22");
        strings.add("16:37:16");
        strings.add("16:38:44");
        strings.add("16:43:11");
        strings.add("16:44:19");
        strings.add("16:45:00");
        strings.add("16:47:56");
        strings.add("16:47:56");
        strings.add("16:48:39");
        strings.add("16:48:59");
        strings.add("16:50:00");
        strings.add("16:50:41");
        strings.add("16:51:58");
        strings.add("16:53:33");
        strings.add("16:54:40");
        strings.add("16:55:04");
        strings.add("16:56:50");
        strings.add("16:57:07");
        strings.add("16:57:57");
        strings.add("16:58:01");
        strings.add("16:58:03");
        strings.add("16:59:19");
        strings.add("16:59:48");
        strings.add("17:00:00");
        strings.add("17:00:11");
        strings.add("17:02:21");
        strings.add("17:03:09");
        strings.add("17:03:26");
        strings.add("17:03:54");
        strings.add("17:04:00");
        strings.add("17:05:06");
        strings.add("17:08:51");
        strings.add("17:09:36");
        strings.add("17:11:38");
        strings.add("17:13:15");
        strings.add("17:13:57");
        strings.add("17:14:48");
        strings.add("17:15:09");
        strings.add("17:16:18");
        strings.add("17:16:23");
        strings.add("17:17:28");
        strings.add("17:17:33");
        strings.add("17:19:58");
        strings.add("17:20:31");
        strings.add("17:21:31");
        strings.add("17:22:12");
        strings.add("17:24:52");
        strings.add("17:25:44");
        strings.add("17:25:55");
        strings.add("17:26:30");
        strings.add("17:27:07");
        strings.add("17:27:44");
        strings.add("17:27:51");
        strings.add("17:30:20");
        strings.add("17:31:30");
        strings.add("17:31:49");
        strings.add("17:32:21");
        strings.add("17:33:29");
        strings.add("17:34:09");
        strings.add("17:35:04");
        strings.add("17:35:44");
        strings.add("17:37:01");
        strings.add("17:37:35");
        strings.add("17:38:24");
        strings.add("17:40:23");
        strings.add("17:41:27");
        strings.add("17:43:54");
        strings.add("17:44:10");
        strings.add("17:46:12");
        strings.add("17:48:27");
        strings.add("17:49:25");
        strings.add("17:50:05");
        strings.add("17:52:07");
        strings.add("17:52:34");
        strings.add("17:54:14");
        strings.add("17:54:48");
        strings.add("17:55:08");
        strings.add("17:55:25");
        strings.add("17:55:59");
        strings.add("17:56:18");
        strings.add("17:56:28");
        strings.add("17:57:14");
        strings.add("17:57:19");

        PeriodAbandonInfo pai = (PeriodAbandonInfo)abandonment.getAt(0);
        DistributionExponential dist = (DistributionExponential)pai.getAt(0).getAbandonTime();
        PeriodCallInfo pci = (PeriodCallInfo)calls.getAt(0);
        Skill skill = pci.getAt(0).getSkill();
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        
        try {                    
            for(int i=0; i<strings.size(); i++){
                
                Date start = formatter.parse(strings.get(i).toString());
                cal.setTime(start);                
                CallInfo ci = new CallInfo(id++, skill, cal.getTime());
                ci.setAbandonTime(dist.getRandom());
                //ci.setCallCentre(((CallCentre)resources.get(0)).getName());
                this.globalQueue.add(ci);                
            }
        } catch (ParseException ex) {
            Logger.getLogger(ACDProject.class.getName()).log(Level.SEVERE, null, ex);
        }        
        Collections.sort(globalQueue, new CallComparator());
        this.totalCalls = globalQueue.size();
    }
    
    //</editor-fold>
}
