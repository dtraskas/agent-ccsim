package com.ccsim;

/*
 * HandlerPlugin.java
 *
 * Created on 14 Jul 2008, 22:50
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

/*
 *  Cougaar specific imports
 */

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 *
 * @author dimitriostraskas
 */
public class HandlerPlugin extends ComponentPlugin
{
    private UIDService uids;
    private MessageAddress sim;
    private MessageAddress logger;
    
    private Vector handlingTimes;
    private Vector workingTimes;
    private String callCentre;
    private HashMap clusters;
    private int totalHandlers;
    private int pongReplies;
    private boolean bBootstrappingComplete;
    private HashMap callQueuePerSkill;
    private Date nextAvailable;
    private Date shiftEnd;
    private Date currentTime;
    private SkillGroup skillgroup;
    private HashMap lastAvailableTags;
    
    // Cougaar subscriptions
    private IncrementalSubscription simDataReceiver;
    private IncrementalSubscription simRegisterReceiver;
    private IncrementalSubscription simCallReceiver;
    private IncrementalSubscription callForwardReceiver;
    private IncrementalSubscription callTransferReceiver;
    private IncrementalSubscription pingReceiver;
    private IncrementalSubscription pongReceiver;
    private IncrementalSubscription updateReceiver;
    
    private LoggingService log;
    private Random rnd;
    
    @Override
    public void load()
    {
        super.load();
        
        rnd = new Random();
        clusters = new HashMap();
        pongReplies = 1;        
        bBootstrappingComplete = false;
        
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
        sendMsg(sim, new InfoPacket("DATA_REQUEST", null));
        // Subscribe to the Sim Agent relays
        simDataReceiver = (IncrementalSubscription)blackboard.subscribe(predDataReceiver());
        // listens to the simulation agent for registration requests
        simRegisterReceiver = (IncrementalSubscription)blackboard.subscribe(predRegisterReceiver());
        // Subscribe to the Sim Agent reply relays        
        simCallReceiver = (IncrementalSubscription)blackboard.subscribe(predSimCallReceiver());                 
        
        // Subscribe to the Handler Agent reply relays        
        callForwardReceiver = (IncrementalSubscription)blackboard.subscribe(predCallForwardReceiver());                 
        callTransferReceiver = (IncrementalSubscription)blackboard.subscribe(predCallTransferReceiver());                 
        
        // listens for ping messages from other agents
        pingReceiver = (IncrementalSubscription)blackboard.subscribe(predPingReceiver());
        // listens for pong messages from other agents
        pongReceiver = (IncrementalSubscription)blackboard.subscribe(predPongReceiver());
        // listens for update messages from other agents
        updateReceiver = (IncrementalSubscription)blackboard.subscribe(predUpdateReceiver());
    }

    @Override
    protected void execute() 
    {        
        // observe Simulator reply relays for data messages
        if (simDataReceiver.hasChanged()) getData();

        // observe the simulator agent for registration requests
        if (simRegisterReceiver.hasChanged()) bootstrapStepOne();

        // observe the handler agents for pong replies and initiate the second step of bootstraping
        if (pongReceiver.hasChanged()) bootstrapStepTwo();                       

        // during this step all information collected is used for cluster formation
        if (pongReplies == totalHandlers && !bBootstrappingComplete) bootstrapStepThree();            
        
        // observe the handler agents for pings at any point in time
        if (pingReceiver.hasChanged()) pongPinger();
        
        // receives calls from simulator or other agents       
        if (bBootstrappingComplete) {
            if (simCallReceiver.hasChanged()) processCall(receiveSimCall());
            if (callForwardReceiver.hasChanged()) processCall(receiveCallForward());
            if (callTransferReceiver.hasChanged()) allocateCall(receiveCallTransfer());
            if (updateReceiver.hasChanged()) receiveUpdate();
        }
    }
    
    //<editor-fold desc="Bootstrapping Process">
    
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
            
            // reset the call queue for each skill
            callQueuePerSkill = new HashMap();
            for(int i=0; i<this.skillgroup.getSkillCount(); i++){
                callQueuePerSkill.put(this.skillgroup.getSkillAt(i).getName(), new Vector());                
            }
            // reset the tags for each of the skills
            lastAvailableTags = new HashMap();
            
            // adjust agent availability
            this.nextAvailable = ((PeriodShiftInfo)workingTimes.get(0)).getStart();
            this.shiftEnd = getShiftEnd(0);
        }
    }
    
    private void pongPinger()
    {
        for (Iterator iter = pingReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay) iter.next();
            MessageAddress handler = relay.getSource();
            
            sendMsg(handler, new InfoPacket("PONG", new HandlerPacket(agentId.getAddress(),nextAvailable,shiftEnd,skillgroup)));
        }
    }
    
    private void bootstrapStepOne()
    {
        // step 1: ping each node in the system
        for(int i=1; i<=totalHandlers; i++){
            MessageAddress handler = MessageAddress.getMessageAddress("HA_" + i);      
            if (handler != agentId) {
                sendMsg(handler, new InfoPacket("PING", null));
            }
        }                
    }
    
    private void bootstrapStepTwo()
    {
        for (Iterator iter = pongReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay) iter.next();
            InfoPacket packet = (InfoPacket) relay.getQuery();
            HandlerPacket hp = (HandlerPacket)packet.getContent();
            SkillGroup sg = hp.getSkillGroup();
            
            for(int i=0; i<sg.getSkillCount(); i++){
                Skill skill = sg.getSkillAt(i);
                
                if (clusters.containsKey(skill.getName())) {
                    ((Vector) clusters.get(skill.getName())).add(hp);
                } else {
                    Vector handlers = new Vector();
                    handlers.add(hp);
                    clusters.put(skill.getName(), handlers);
                }
            }
            pongReplies++;
        }
    }
    
    private void bootstrapStepThree()
    {
        for(int i=0; i<this.skillgroup.getSkillCount(); i++){
            sortCluster(this.skillgroup.getSkillAt(i));
        }
        sendMsg(sim, new InfoPacket("REGISTERED", null));        
        bBootstrappingComplete = true;
    }
    //</editor-fold>
    
    //<editor-fold desc="Call Processing Functions">
    
    private void processCall(CallInfo call)
    {
        Skill skill = call.getSkill();        
        // first check if the call is at the right cluster, otherwise forward to a handler within another cluster.
        if (isRightCluster(skill)) {     
            //log.shout("[processCall] Call(" + call.getId() + ") at the right cluster");
            // check if the current agent is marked as the last available one
            if (isLastAvailable(skill)){
                //log.shout("[processCall] Call(" + call.getId() + ") at the last available agent");
                // the routing log stored in the call is backed up and a new current log is created before the call is queued.
                call.resetLocalRoutingLog();
                
                // check if there are other calls in the priority queue
                queueCall(call);
                CallInfo nextCall = getNextCall(skill);
                
                // attempt to allocate call to the next available agent if current agent is on a break
                // send the call and updates of the current calls queue and cluster information
                if (isOnShift()) {         
                    // handle the top call on the list and remove it from the queue,                     
                    handleCall(nextCall);                    
                    // check abandonment
                    checkAbandonment(skill);
                    // resort the current cluster and notify the next available agent(s).
                    sortCluster(skill);
                    // send update messages
                    updateLastAvailable(skill);
                    // check the queue of calls for other calls to be handled
                    Vector calls = (Vector)callQueuePerSkill.get(skill.getName());
                    if(calls.size() > 0) {
                        //log.shout("[processCall] Call(" + nextCall.getId() + ") processCallFromQueue");
                        processCallFromQueue(getNextCall(skill));                        
                    } else {
                        //log.shout("Asking Simulator for next Call");
                        sendMsg(sim, new InfoPacket("NEXT_CALL", null));
                    }    
                } else {
                    transferCall(nextCall);                                                        
                }                
            } else {
                // check which handlers of the current cluster are visited before sending a call message
                forwardCall(call, true);
            }
        } else {            
            // send call to an agent at a different cluster
            forwardCall(call, false);
        }
    }
   
    private void transferCall(CallInfo call)
    {
        Vector neighbours = (Vector)clusters.get(call.getSkill().getName());
        Vector calls = (Vector)callQueuePerSkill.get(call.getSkill().getName());

        int handlerIndex = findNonVisitedHandler(call, neighbours);
        if (handlerIndex >= 0) {
            MessageAddress handler = MessageAddress.getMessageAddress(((HandlerPacket)neighbours.get(handlerIndex)).getHandlerID());
            Object[] data = new Object[3];
            data[0] = neighbours;
            data[1] = calls;
            data[2] = call;
            
            //log.shout("[transferCall] Call(" + call.getId() + ") transferred to " + handler);
            sendMsg(handler, new InfoPacket("TRANSFER_CALL", data));       
        } else {
            //log.shout("[transferCall] Call(" + call.getId() + ") queued");
            queueCall(call);
            checkAbandonment(call.getSkill());
            updateLastAvailable(call.getSkill());
            sendMsg(sim, new InfoPacket("NEXT_CALL", null));
        }
    }
    
    private void forwardCall(CallInfo call, boolean sameCluster)
    {
        if (sameCluster) {
            Vector neighbours = (Vector)clusters.get(call.getSkill().getName());
            int handlerIndex = findNonVisitedHandler(call, neighbours);
            if (handlerIndex >= 0) {
                MessageAddress handler = MessageAddress.getMessageAddress(((HandlerPacket)neighbours.get(handlerIndex)).getHandlerID());
                sendMsg(handler, new InfoPacket("FORWARD_CALL", call));  
                //log.shout("[forwardCall] Call(" + call.getId() + ") to next Handler:" + handler.getAddress());
            } else {
                //log.shout("[forwardCall] Call(" + call.getId() + ") queued");
                queueCall(call);
                checkAbandonment(call.getSkill());
                updateLastAvailable(call.getSkill());
                sendMsg(sim, new InfoPacket("NEXT_CALL", null));
            }
        } else {            
            MessageAddress handler = MessageAddress.getMessageAddress(getRandomHandlerId(call));
            //log.shout("[forwardCall] Call(" + call.getId() + ") sent to random Handler:" + handler.getAddress());
            sendMsg(handler, new InfoPacket("FORWARD_CALL", call));        
        }
    }
    
    private void processCallFromQueue(CallInfo call)
    {
        if (isLastAvailable(call.getSkill())){
            allocateCall(call);                  
        } else {
            // check if all handlers of the current cluster are visited
            forwardCall(call, true);
        }
    }
    
    private void allocateCall(CallInfo call)
    {
        // the routing log stored in the call is backed up and a new current log is created before the call is queued.
        //call.resetLocalRoutingLog();

        // attempt to allocate call to the next available agent if current agent is on a break
        // send the call and updates of the current calls queue and cluster information
        if (isOnShift()) {                    
            // handle the top call on the list and remove it from the queue,                     
            handleCall(call);                    
            // check abandonment
            checkAbandonment(call.getSkill());
            // resort the current cluster and notify the next available agent(s).
            sortCluster(call.getSkill());
            // send update messages
            updateLastAvailable(call.getSkill());
            // check the queue of calls for other calls to be handled
            Vector calls = (Vector)callQueuePerSkill.get(call.getSkill().getName());
            if(calls.size() > 0) {
                //log.shout("[allocateCall] Call(" + call.getId() + ") processCallFromQueue");
                processCallFromQueue(getNextCall(call.getSkill()));
            } else {
                //log.shout("Asking Simulator for next Call");
                sendMsg(sim, new InfoPacket("NEXT_CALL", null));
            } 
        } else {
            transferCall(call);            
        }                  
    }
    
    private void updateLastAvailable(Skill skill)
    {
        Vector neighbours = (Vector)clusters.get(skill.getName());
        Vector calls = (Vector)callQueuePerSkill.get(skill.getName());
        Object[] data = new Object[3];      
        data[0] = skill;
        data[1] = calls;
        data[2] = neighbours;
        
        HandlerPacket top = (HandlerPacket)neighbours.get(0);                
        
        if (top.getNextAvailable().getTime() <= this.nextAvailable.getTime()) {
            MessageAddress handler = MessageAddress.getMessageAddress(top.getHandlerID());            
            sendMsg(handler, new InfoPacket("UPDATE", data));
            
            for(int i=1; i<neighbours.size(); i++){
                HandlerPacket hp = (HandlerPacket)neighbours.get(i);                
                if (hp.getNextAvailable().getTime() == top.getNextAvailable().getTime()) {
                    handler = MessageAddress.getMessageAddress(hp.getHandlerID());            
                    sendMsg(handler, new InfoPacket("UPDATE", data));
                    //log.shout("Updating Handler: " + handler.getAddress());
                } else {
                    break;
                }
            }
        }            
    }
    
    private void handleCall(CallInfo call)
    {
        int periodIndex = findPeriod(call.getArrival());
        if (periodIndex >= 0) {
            PeriodHandleInfo phi = (PeriodHandleInfo)handlingTimes.get(periodIndex);
            HandleInfo hi = phi.getAt(call.getSkill());            
            double handleTime = ((DistributionExponential)hi.getHandleTime()).getRandom();
            double answerTime = (((double)call.getCurrentTime().getTime() - (double)call.getArrival().getTime()) / 1000);
            
            call.setHandleTime(handleTime);
            call.setAnswerTime(answerTime);
            call.setAgentHandlerID(agentId.getAddress());
            call.setAbandonTime(0);
            
            // once handled remove the call from the callQueue.
            Vector calls = (Vector)callQueuePerSkill.get(call.getSkill().getName());
            calls.remove(call);
            logCall(call);
            //log.shout("call handled: " + call.getId());
            
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
    
    private void checkAbandonment(Skill skill)
    {
        Vector calls = (Vector)callQueuePerSkill.get(skill.getName());        
        Vector abandonedCalls = new Vector();
        
        Calendar cal = new GregorianCalendar();
        int x = 0;
        while(x < calls.size()){
            CallInfo call = (CallInfo)calls.get(x++);
            cal.setTime(call.getArrival());
            cal.add(Calendar.SECOND, (int)call.getAbandonTime());

            if (cal.getTime().getTime() <= currentTime.getTime()) {
                abandonedCalls.add(call);                        
            }
        }

        for(int i=0; i<abandonedCalls.size(); i++){
            CallInfo call = (CallInfo)abandonedCalls.get(i);
            calls.remove(call);            
            logCall(call);
            //log.shout("call abandoned: " + call.getId());
        }
    }
    
    private void sortCluster(Skill skill)
    {
        String skillName = skill.getName();
        Vector neighbours = (Vector)clusters.get(skillName);
        Collections.sort(neighbours, new HandlerPacketComparator());
        
        boolean bIsLastAvailable = (((HandlerPacket)neighbours.get(0)).getNextAvailable().getTime() >= this.nextAvailable.getTime());        
        lastAvailableTags.put(skillName, bIsLastAvailable);
    }
    
    private String getRandomHandlerId(CallInfo call)
    {       
        Vector cluster = (Vector)clusters.get(call.getSkill().getName());
        int index = rnd.nextInt(cluster.size());
 
        return ((HandlerPacket)cluster.get(index)).getHandlerID();
    }
    
    private boolean isRightCluster(Skill skill)
    {
        for(int x=0; x<skillgroup.getSkillCount(); x++){
            Skill s = skillgroup.getSkillAt(x);
            if (s.getName().compareTo(skill.getName()) == 0) return true;
        }
        return false;
    }

    private boolean isLastAvailable(Skill skill)
    {        
        return Boolean.parseBoolean(this.lastAvailableTags.get(skill.getName()).toString());  
    }
    
    private boolean isOnShift()
    {
        //log.shout(nextAvailable.toString());
        return (currentTime.getTime() >= nextAvailable.getTime() && currentTime.getTime() < shiftEnd.getTime());
    }
    
    private int findNonVisitedHandler(CallInfo call, Vector handlers)
    {
        for(int i=0; i<handlers.size(); i++){
            HandlerPacket hp = (HandlerPacket)handlers.get(i);    
            if (hp.getHandlerID().compareTo(agentId.getAddress()) != 0 && !call.hasVisitedHandler(hp.getHandlerID())) return i;
        }
        return -1;
    }
    
    private void queueCall(CallInfo call)
    {
        Vector calls = (Vector)callQueuePerSkill.get(call.getSkill().getName());
        calls.add(call);
        Collections.sort(calls, new CallComparator());
    }
    
    private CallInfo getNextCall(Skill skill)
    {        
        CallInfo call = (CallInfo)((Vector)callQueuePerSkill.get(skill.getName())).get(0);
        call.setCurrentTime(currentTime);
        return call;
    }
    
    private void logCall(CallInfo call)
    {
        sendMsg(logger, new InfoPacket("LOG_CALL", call));
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Cougaar Predicates"> 
    
    private UnaryPredicate predDataReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isDataMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("DATA_REQUEST") == 0);
                    
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
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isCallMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predCallTransferReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isTransferMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("TRANSFER_CALL") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isTransferMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predCallForwardReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isForwardMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("FORWARD_CALL") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isForwardMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predUpdateReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isUpdateMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("UPDATE") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isUpdateMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predRegisterReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                    
                    boolean isRegisterMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("REGISTER_PING") == 0);                    
                    
                    if (sim.equals(relay.getSource()) && isRegisterMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predPingReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                    
                    boolean isPingMessage = (relay.getTarget() == agentId && ((InfoPacket)relay.getQuery()).getSubject().compareTo("PING") == 0); 
                    
                    if (isPingMessage) return true;
                }
                return false;
            }
        };
    }
     
    private UnaryPredicate predPongReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;        
                    boolean isPongMessage = (relay.getTarget() == agentId && ((InfoPacket)relay.getQuery()).getSubject().compareTo("PONG") == 0);                    
                    
                    if (isPongMessage) return true;
                }
                return false;
            }
        };
    }
    //</editor-fold>
    
    //<editor-fold desc="Receiver Functions"> 
    
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
        }
        return call;
    }
    
    private CallInfo receiveCallForward()
    {
        // the handler has just received a new message indicating that there is a call in the queue.
        CallInfo call = null;
        for (Iterator iter = callForwardReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getQuery();
            call = (CallInfo)packet.getContent();            
            call.logRoute(agentId.getAddress());
            updateClock(call);            
        }
        return call;
    }
    
    private CallInfo receiveCallTransfer()
    {
        // the handler has just received a new message indicating that there is a call in the queue.
        CallInfo call = null;
        for (Iterator iter = callTransferReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getQuery();
            Object[] data = (Object[])packet.getContent();
            
            call = (CallInfo)data[2];
            clusters.put(call.getSkill().getName(), (Vector)data[0]);
            callQueuePerSkill.put(call.getSkill().getName(), (Vector)data[1]);
            
            call.logRoute(agentId.getAddress());
            updateClock(call);            
        }
        return call;
    }
    
    private void receiveUpdate()
    {
        // the handler has just received an update message.
        for (Iterator iter = updateReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getQuery();            
            Object[] data = (Object[])packet.getContent();
                    
            Skill skill = (Skill)data[0];
            
            callQueuePerSkill.put(skill.getName(), (Vector)data[1]);
            clusters.put(skill.getName(), (Vector)data[2]);            
        }
    }
    //</editor-fold> 
    
    //<editor-fold desc="Time Utilities">
    
    private void updateClock(CallInfo call)
    {
        this.currentTime = call.getArrival();
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
    
    private void sendMsg(MessageAddress target,  Object content) 
    {
        // Send a new relay to the target
        SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
        blackboard.publishAdd(relay);
    }
    
    private void replyTo(SimpleRelay relay, Object content) 
    {
        // Send back the same content as our response
        relay.setReply(content);         
        blackboard.publishChange(relay);
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
