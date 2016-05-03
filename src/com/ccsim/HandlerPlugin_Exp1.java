package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * HandlerPlugin_Exp1.java
 *
 * Created on 27/07/2008, 21:15
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */

import java.io.File;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;


/////////////////////////////////////////////////////////
//  Cougaar specific imports
/////////////////////////////////////////////////////////
 
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

public class HandlerPlugin_Exp1 extends ComponentPlugin
{
    private UIDService uids;
    private MessageAddress sim;
    private MessageAddress logger;
    
    private Vector handlingTimes;
    private Vector workingTimes;
    private String callCentre;
    private int totalHandlers;
    private Date nextAvailable;
    private Date shiftEnd;
    private Date currentTime;
    private SkillGroup skillgroup;   
    
    // Cougaar subscriptions
    private IncrementalSubscription simDataReceiver;
    private IncrementalSubscription simCallReceiver;
    private IncrementalSubscription simLogRequestReceiver;
    private IncrementalSubscription simResetReceiver;    
    
    private LoggingService log;
    private Random rnd;
    private MessageLogger msgLogger;
    
    @Override
    public void load()
    {
        super.load();
        
        rnd = new Random();
        msgLogger = new MessageLogger(this.agentId.getAddress());
        log = (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);         
        sim = MessageAddress.getMessageAddress("dtraskas_jvm");
        logger = MessageAddress.getMessageAddress("logger");
        uids = (UIDService)getServiceBroker().getService(this, UIDService.class, null);
    }
    
    @Override
    protected void setupSubscriptions() 
    {
        // first remove the XML file that sets up the Agent
        removeComponentFile();
        // now request the shift and handle time information from the Simulation Agent
        sendMsg(sim, new InfoPacket("DATA_REQUEST", null), MessageType.DataRequest);        
        // Subscribe to the Sim Agent relays
        simDataReceiver = (IncrementalSubscription)blackboard.subscribe(predDataReceiver());
        // Subscribe to the Sim Agent reply relays        
        simCallReceiver = (IncrementalSubscription)blackboard.subscribe(predSimCallReceiver());        
        // Subscribe to the Sim Agent log request relays        
        simLogRequestReceiver = (IncrementalSubscription)blackboard.subscribe(predSimLogRequestReceiver());        
        // Subscribe to the Sim Agent reset relays        
        simResetReceiver = (IncrementalSubscription)blackboard.subscribe(predSimResetReceiver());                       
    }

    @Override
    protected void execute() 
    {        
        if (simResetReceiver.hasChanged()) receiveResetCall();
        if (simDataReceiver.hasChanged()) getData();        
        if (simCallReceiver.hasChanged()) processCall(receiveSimCall());        
        if (simLogRequestReceiver.hasChanged()) sendMsg(logger, new InfoPacket("LOG_MESSAGE_COUNT", msgLogger), MessageType.LogMessageCount);
    }
    
    //<editor-fold desc="Call Allocation Routines">
    
    private void processCall(CallInfo call)
    {
        Skill skill = call.getSkill();        
        // first check if the call is at the right cluster, otherwise forward to a handler within another cluster.
        if (canHandleSkill(skill)) {     
            if (isOnShift()) {         
                // handle the top call on the list and remove it from the queue
                handleCall(call);                                    
            } else {
                abandonCall(call);
            }                            
        } else {
            abandonCall(call);
        }
        sendMsg(sim, new InfoPacket("NEXT_CALL", null), MessageType.NextCallRequest);    
    }
   
    private void handleCall(CallInfo call)
    {
        int periodIndex = findPeriod(call.getArrival());
        if (periodIndex >= 0) {
            PeriodHandleInfo phi = (PeriodHandleInfo)handlingTimes.get(periodIndex);
            HandleInfo hi = phi.getAt(call.getSkill());            
            double handleTime = ((DistributionExponential)hi.getHandleTime()).getRandom();            
            double callCurrentTime = (double)call.getCurrentTime().getTime();
            double callArrivalTime = (double)call.getArrival().getTime();
            double answerTime = 0;
            
            if (callCurrentTime > callArrivalTime) answerTime = ((callCurrentTime - callArrivalTime) / 1000);
            
            call.setHandleTime(handleTime);
            call.setAnswerTime(answerTime);
            call.setAgentHandlerID(agentId.getAddress());
            call.setAbandonTime(0);
            
            // once handled remove the call from the callQueue.
            logCall(call);
            
            // calculate the next available time.
            Calendar cal = new GregorianCalendar();
            cal.setTime(call.getArrival());
            cal.add(Calendar.SECOND, (int)(answerTime + handleTime));
            this.nextAvailable = cal.getTime();

            PeriodShiftInfo shift = (PeriodShiftInfo)workingTimes.get(periodIndex);
            if (this.nextAvailable.getTime() >= shift.getFinish().getTime()) {
                periodIndex++;
                if (periodIndex < workingTimes.size()) {
                    this.nextAvailable = ((PeriodShiftInfo)workingTimes.get(periodIndex)).getStart();
                    this.shiftEnd = getShiftEnd(periodIndex);
                }
            }
        }
    }
    
    private void abandonCall(CallInfo call)
    {        
        call.setAgentHandlerID(agentId.getAddress());
        logCall(call);        
    }
    
    private boolean isOnShift()
    {
        return (currentTime.getTime() >= nextAvailable.getTime() && currentTime.getTime() < shiftEnd.getTime());
    }
    
    private void logCall(CallInfo call)
    {
        sendMsg(logger, new InfoPacket("LOG_CALL", call), MessageType.LogCall);
    }
    
    private boolean canHandleSkill(Skill skill)
    {
        for(int x=0; x<skillgroup.getSkillCount(); x++){
            Skill skill2 = skillgroup.getSkillAt(x);
            
            if (skill2.getName().compareTo(skill.getName()) == 0) return true;
        }
        return false;
    }
    //</editor-fold>
    
    //<editor-fold desc="Receivers">
    
    private UnaryPredicate predDataReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isDataMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("DATA_REQUEST") == 0);
                    msgLogger.AddReceived();
                    
                    if (agentId.equals(relay.getSource()) && sim.equals(relay.getTarget()) && isDataMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predSimCallReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isCallMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("NEW_CALL") == 0);
                    msgLogger.AddReceivedCall();
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isCallMessage) return true;
                }
                return false;
            }
        };
    }    
    
    private UnaryPredicate predSimLogRequestReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isLogMessageCount = (((InfoPacket)relay.getQuery()).getSubject().compareTo("LOG_MESSAGE_COUNT") == 0);
                    msgLogger.AddReceived();
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isLogMessageCount) return true;
                }
                return false;
            }
        };
    }    
    
    private UnaryPredicate predSimResetReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isResetMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("RESET") == 0);
                    msgLogger.AddReceived();
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isResetMessage) return true;
                }
                return false;
            }
        };
    }    
    
    private void getData()
    {
        for (Iterator iter = simDataReceiver.getChangedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getReply();

            // always remove a relay that has been sent back as a reply.
            blackboard.publishRemove(relay);

            // extract the data needed.
            Object[] data = (Object[])packet.getContent();
            this.handlingTimes = (Vector)data[0];
            this.workingTimes = (Vector)data[1];
            this.callCentre = (String)data[2];                
            this.totalHandlers = Integer.parseInt(data[3].toString());
            this.skillgroup = ((PeriodShiftInfo)workingTimes.get(0)).getSkillgroup();
            
            resetAvailability();
            
            sendMsg(sim, new InfoPacket("REGISTERED", null), MessageType.Registered);
        }
    }
    
    private CallInfo receiveSimCall()
    {
        // the handler has just received a new message indicating that there is a call in the queue.
        CallInfo call = null;
        for (Iterator iter = simCallReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getQuery();
            call = (CallInfo)packet.getContent();   
            call.logRoute(agentId.getAddress());              
            
            updateClock(call);
            call.setCurrentTime(currentTime);
            log.shout("Call message received:" + call.getArrival().toString());
        }
        return call;
    }
    
    private void receiveResetCall()
    {
        for (Iterator iter = simResetReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            iter.next();
            resetAvailability();
            log.shout("Reset message received");
        }
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Time Utilities">
    
    private void updateClock(CallInfo call)
    {
        this.currentTime = call.getArrival();
    }
    
    private void resetAvailability()
    {
        this.nextAvailable = ((PeriodShiftInfo)workingTimes.get(0)).getStart();
        this.shiftEnd = getShiftEnd(0);
        this.currentTime = new Date(0);
    }
    
    private Date getShiftEnd(int index)
    {
        Date finish = ((PeriodShiftInfo)workingTimes.get(index)).getFinish();

        int curIndex = index + 1;            
        while(curIndex < workingTimes.size()){
            Date start = ((PeriodShiftInfo)workingTimes.get(curIndex)).getStart();
            if (start.getTime() > finish.getTime()) return finish;
            finish = ((PeriodShiftInfo)workingTimes.get(curIndex)).getFinish();

            curIndex++;
        }
        return finish;
    }

    private int findPeriod(Date time)
    {
        for(int x=0; x<workingTimes.size(); x++){
            Date start = ((PeriodShiftInfo)workingTimes.get(x)).getStart();
            Date finish = ((PeriodShiftInfo)workingTimes.get(x)).getFinish();

            if (start.getTime() <= time.getTime() && time.getTime() < finish.getTime()) {
                return x;
            }
        }
        return -1;
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Messaging Utilities">
    
    private void sendMsg(MessageAddress target,  Object content, MessageType msgType) 
    {
        //log the message count
        switch(msgType)
        {
            case DataRequest:
            case NextCallRequest:
            case LogCall:
            case Registered:
                msgLogger.AddSent();
                break;
        }
        
        // Send a new relay to the target
        SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
        blackboard.publishAdd(relay);
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Various Utility Functions"> 
    
    private void removeComponentFile()
    {
        File file = new File(getAgentIdentifier() + ".xml");
        if (file.exists()) {
            file.delete();
        }
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
    
    private class CallComparator implements Comparator 
    {
        public int compare(Object o1, Object o2) 
        {
            CallInfo ci1 = (CallInfo)o1;
            CallInfo ci2 = (CallInfo)o2;
              
            int priorityDiff = ci1.getSkill().getPriority() - ci2.getSkill().getPriority();
            int timeDiff = (int)ci1.getArrival().getTime() - (int)ci2.getArrival().getTime();

            if (priorityDiff == 0) {
                return timeDiff;
            } else {
                return priorityDiff;
            }
        }
    }
    
    //</editor-fold>
}
