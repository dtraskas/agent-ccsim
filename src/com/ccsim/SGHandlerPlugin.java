package com.ccsim;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * SGHandlerPlugin.java
 *
 * Created on 01/09/2008, 22:03
 * 
 * Dimitrios Traskas
 * Bath University 2008
 * 
 */

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
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

public class SGHandlerPlugin extends ComponentPlugin
{
    private UIDService uids;
    private MessageAddress sim;
    private MessageAddress logger;
    
    private DateFormat df;
    private boolean isLastCall;
    private Vector handlingTimes;
    private Vector workingTimes;
    private String callCentre;
    private int totalHandlers;
    private Date nextAvailable;
    private Date shiftEnd;
    private Date currentTime;
    private SkillGroup skillgroup;   
    private int pongReplies;
    private boolean bBootstrappingComplete;
    private Vector callQueue;    
    private Vector cluster;
    private ForwardList flist;
    private MessageAddress nextAgent;
    
    // Cougaar subscriptions
    private IncrementalSubscription simDataReceiver;
    private IncrementalSubscription simResetReceiver;
    private IncrementalSubscription simCallReceiver;
    private IncrementalSubscription simBoostrapRequestReceiver;
    private IncrementalSubscription simLogRequestReceiver;    
    
    private IncrementalSubscription callForwardReceiver;
    private IncrementalSubscription callAbandonReceiver;
    private IncrementalSubscription callAllocateReceiver;
    
    private IncrementalSubscription pingReceiver;
    private IncrementalSubscription pongReceiver;
    
    private LoggingService log;
    private Random rnd;
    private MessageLogger msgLogger;
    
    @Override
    public void load()
    {
        super.load();
        
        df = new SimpleDateFormat("HH:mm:ss");
        rnd = new Random();
        pongReplies = -1;        
        bBootstrappingComplete = false;
        
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
        simResetReceiver = (IncrementalSubscription)blackboard.subscribe(predSimResetReceiver());                       
        simCallReceiver = (IncrementalSubscription)blackboard.subscribe(predCallReceiver()); 
        simBoostrapRequestReceiver = (IncrementalSubscription)blackboard.subscribe(predBootstrapRequestReceiver());
        
        callForwardReceiver = (IncrementalSubscription)blackboard.subscribe(predCallForwardReceiver());
        callAbandonReceiver = (IncrementalSubscription)blackboard.subscribe(predCallAbandonReceiver());
        callAllocateReceiver = (IncrementalSubscription)blackboard.subscribe(predCallAllocateReceiver());
        
        pingReceiver = (IncrementalSubscription)blackboard.subscribe(predPingReceiver());
        pongReceiver = (IncrementalSubscription)blackboard.subscribe(predPongReceiver());
        
        // Subscribe to the Sim Agent log request relays        
        //simLogRequestReceiver = (IncrementalSubscription)blackboard.subscribe(predSimLogRequestReceiver());                
    }

    @Override
    protected void execute() 
    {        
        if (simResetReceiver.hasChanged()) {
            //log.shout("received reset call");
            receiveResetCall();
        }
        if (simDataReceiver.hasChanged()) {
            //log.shout("received data call");
            receiveData();
        }      
        
        // begin the bootstrapping sequence 
        if (simBoostrapRequestReceiver.hasChanged()) {
            //log.shout("bootstrap one");
            bootstrapStepOne();
        }
        
        if (pongReceiver.hasChanged()) {
            //log.shout("bootstrap two");
            bootstrapStepTwo();
        }
        
        if (pongReplies == totalHandlers && !bBootstrappingComplete) {
            //log.shout("bootstrap three");
            bootstrapStepThree();
        }   
        
        if (pingReceiver.hasChanged()) {
            //log.shout("pong pinger");
            pongPinger();
        }
        
        if (bBootstrappingComplete) {
            if (simCallReceiver.hasChanged()) {
                //log.shout("process new call");                
                processNewCall(receiveCall()); 
            }
            
            if (callForwardReceiver.hasChanged()) {
                //log.shout("process forwarded call");
                receiveForwardList();                
                processForwardedCall(flist.getCurrentCall());                
            }
            
            if (callAbandonReceiver.hasChanged()) {
                //log.shout("process abandon command");
                receiveAbandonCommand();
                processAbandonCommand();
            }
            
            if (callAllocateReceiver.hasChanged()) {
                //log.shout("process allocate command");
                processAllocateCommand();
            }
        }
        
        //if (simLogRequestReceiver.hasChanged()) sendMsg(logger, new InfoPacket("LOG_MESSAGE_COUNT", msgLogger), MessageType.LogMessageCount);
    }
    
    //<editor-fold desc="Call Allocation Routines">
    
    private void processNewCall(CallInfo call)
    {        
        // first check if the call is at the right cluster, otherwise don't do anything
        CallInfo processCall = call;
        if (isRightCluster(call.getSkill())){
            queueCall(call);   
            processCall = getNextCall();            
            this.flist.setCurrentCall(processCall);
            this.flist.setCurrentTime(currentTime);
            checkAvailability(processCall);            
            forwardCall(getNextAgent(), flist);
        } else {
            if (callQueue.size() > 0){
                processCall = getNextCall();
                this.flist.setCurrentCall(processCall);
                this.flist.setCurrentTime(currentTime);
                checkAvailability(processCall);                
                forwardCall(getNextAgent(), flist);
            } else {
                sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);                
            }
        }        
    }
    
    private void processForwardedCall(CallInfo call)
    {
        if (isFirstAgent()) {
            if (flist.getAvailableCount() > 0){
                flist.sortHandlers();
                String bestHandlerId = flist.getBestHandlerId();
                if (bestHandlerId.compareTo(agentId.getAddress()) == 0) {                                        
                    handleCall(call);                    
                    if (cluster.size() > 1) {
                        forwardAllocateCall(getNextAgent(), flist);
                    } else {
                        if (isLastCall) {
                            abandonRemainingCalls();
                        } else {
                            checkAbandonment();
                            removeAbandonedCalls(true); 
                        }
                        sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
                    }
                } else {
                    callQueue.remove(0);
                    forwardAllocateCall(getNextAgent(), flist);                    
                }
            } else {
                if (isLastCall) abandonRemainingCalls();
                sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
            }
        } else {            
            if (!callInQueue(call)) queueCall(call);            
            checkAvailability(call);
            forwardCall(getNextAgent(), flist);
        }
    }
    
    private void processAllocateCommand()
    {           
        if (isFirstAgent()) {
            flist.removeBestHandler();
            if (callQueue.size() > 0) {
                if (flist.getAvailableCount() > 0){
                    CallInfo nextCall = getNextCall();
                    nextCall.setCurrentTime(currentTime);
                    flist.setCurrentCall(nextCall);
                    flist.clearAvailable();
                    checkAvailability(nextCall);
                    forwardCall(getNextAgent(), flist);                    
                } else {
                    if (isLastCall) {
                        abandonRemainingCalls();
                    } else {
                        checkAbandonment();
                        if (flist.getAbandonedCount() > 0) {
                            forwardAbandonList(getNextAgent(), flist);
                        } else {
                            sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
                        }                    
                    }
                }
            } else {
                sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
            }
        } else if (flist.getBestHandlerId().compareTo(agentId.getAddress()) == 0) {
            handleCall(flist.getCurrentCall());
            forwardAllocateCall(getNextAgent(), flist);
        } else {
            callQueue.remove(0);
            forwardAllocateCall(getNextAgent(), flist);
        }
    }
    
    private void processAbandonCommand()
    {        
        if (isFirstAgent()) {
            removeAbandonedCalls(true);
            sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
        } else {
            removeAbandonedCalls(false);
            forwardAbandonList(getNextAgent(), flist);
        }        
    }
    
    private void checkAvailability(CallInfo call)
    {
        // check if call can be handled at this agent.
        int periodIndex = getHandlePeriod(call);
        //log.shout("Period " + periodIndex + ", currenttime: " + df.format(currentTime) + ", next available: " + df.format(nextAvailable) + ", call: " + df.format(call.getArrival()));
        if (isOnShift() && periodIndex >= 0) {
            //log.shout("add available");
            flist.addAvailable(new HandlerPacket(agentId.getAddress(), nextAvailable, shiftEnd, skillgroup));
        }
    }
    
    private void handleCall(CallInfo call)
    {
        int periodIndex = getHandlePeriod(call);
        PeriodHandleInfo phi = (PeriodHandleInfo)handlingTimes.get(periodIndex);
        HandleInfo hi = phi.getAt(call.getSkill());            
        double handleTime = ((DistributionExponential)hi.getHandleTime()).getRandom();            
        double callArrivalTime = (double)call.getArrival().getTime();
        double answerTime = 0;

        if (nextAvailable.getTime() > callArrivalTime) answerTime = ((nextAvailable.getTime() - callArrivalTime) / 1000);

        call.setHandleTime(handleTime);
        call.setAnswerTime(answerTime);            
        call.setAgentHandlerID(agentId.getAddress());
        call.setAbandonTime(0);        
        
        // once handled remove the call from the callQueue.
        logCall(call);
        callQueue.remove(call);
        //log.shout("Remove call at " + df.format(call.getArrival()));
        
        Calendar cal = new GregorianCalendar();
        cal.setTime(call.getArrival());
        cal.add(Calendar.SECOND, (int)(answerTime + handleTime));
        this.nextAvailable = cal.getTime();

        PeriodShiftInfo shift = (PeriodShiftInfo)workingTimes.get(periodIndex);
        if (this.nextAvailable.getTime() >= shift.getFinish().getTime()) {
            periodIndex++;
            if (periodIndex < workingTimes.size()) {
                this.shiftEnd = getShiftEnd(periodIndex);
            }
        }
    }
    
    private void forwardAllocateCall(MessageAddress handler, ForwardList list)
    {
        sendMsg(handler, new InfoPacket("ALLOCATE_CALL", list), MessageType.CallAllocate);  
    }
    
    private void forwardCall(MessageAddress handler, ForwardList list)
    {
        sendMsg(handler, new InfoPacket("FORWARD_CALL", list), MessageType.CallForward);  
    }    
    
    private void forwardAbandonList(MessageAddress handler, ForwardList list)
    {
        sendMsg(handler, new InfoPacket("ABANDON", list), MessageType.CallAbandon);         
    }
    
    private void checkAbandonment()
    {
        //check for abandonment at this point        
        Calendar cal = new GregorianCalendar();
        int x = 0;
        while(x < callQueue.size()){
            CallInfo call = (CallInfo)callQueue.get(x++);
            cal.setTime(call.getArrival());
            cal.add(Calendar.SECOND, (int)call.getAbandonTime());
            
            if (cal.getTime().getTime() <= currentTime.getTime()) {
                call.setCurrentTime(currentTime);                
                flist.addAbandoned(call);
            }
        }
    }
    
    private void removeAbandonedCalls(boolean bLogAbandon)
    {
        for(int i=0; i<flist.getAbandonedCount(); i++){
            CallInfo call = flist.getAbandonedCall(i);
            int index = findCall(call.getId());
            if (index >= 0) {
                callQueue.remove(index);
                if (bLogAbandon) logCall(call);
            }
        }
    }
    
    private void abandonRemainingCalls()
    {
        int x = 0;
        while(x < callQueue.size()){
            CallInfo call = (CallInfo)callQueue.get(x++);
            call.setCurrentTime(currentTime);
            logCall(call);
        }
    }
    
    
    //</editor-fold>
    
    //<editor-fold desc="Bootstrapping Routines">
    
    private void bootstrapStepOne()
    {        
        // step 1: ping each node in the system irrespective of skill
        for(int i=1; i<=totalHandlers; i++){
            MessageAddress handler = MessageAddress.getMessageAddress("HA_" + i);
            if (handler != agentId) sendMsg(handler, new InfoPacket("PING", null), MessageType.Ping);
        }
        pongReplies = 1;    
    }
    
    private void bootstrapStepTwo()
    {
        // find the neighbours and check their skills
        for (Iterator iter = pongReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay) iter.next();
            InfoPacket packet = (InfoPacket) relay.getQuery();
            SkillGroup sg = (SkillGroup)packet.getContent();
            
            if (this.skillgroup.getName().compareTo(sg.getName()) == 0){
                cluster.add(relay.getSource().getAddress());
            }            
            pongReplies++;
        }        
    }
    
    private void bootstrapStepThree()
    {        
        bBootstrappingComplete = true;
        Collections.sort(cluster);
        // find the next agent
        int cnt = 0;
        boolean found = false;
        while(!found && cnt < cluster.size()){
            String currentAgent = cluster.get(cnt++).toString();
            found = (agentId.getAddress().compareTo(currentAgent) == 0);            
        }
        
        int next = cnt;
        if (next < cluster.size()){
            nextAgent =  MessageAddress.getMessageAddress(cluster.get(next).toString());
        } else {
            nextAgent =  MessageAddress.getMessageAddress(cluster.get(0).toString());
        }
        //log.shout("Next agent is " + nextAgent.getAddress());
        
        String agentAddress = cluster.get(0).toString();
        //log.shout("First agent is: " + agentAddress);
        if (isFirstAgent()) {
            sendMsg(sim, new InfoPacket("BOOTSTRAP_COMPLETE", this.skillgroup), MessageType.BootstrapComplete);        
        } else { 
            sendMsg(sim, new InfoPacket("BOOTSTRAP_COMPLETE", null), MessageType.BootstrapComplete);        
        }
    }
    
    private void pongPinger()
    {
        for (Iterator iter = pingReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay) iter.next();
            MessageAddress handler = relay.getSource();            
            sendMsg(handler, new InfoPacket("PONG", skillgroup), MessageType.Pong);
        }
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Receiver Predicates">
    
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
    
    private UnaryPredicate predDataReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isDataMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("DATA_SEND") == 0);
                    msgLogger.AddReceived();
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isDataMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predBootstrapRequestReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                    
                    boolean isBootstrapRequestMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("START_BOOTSTRAP") == 0);
                    
                    if (sim.equals(relay.getSource()) && isBootstrapRequestMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predCallReceiver() 
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
    
    private UnaryPredicate predCallAbandonReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isAbandonMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("ABANDON") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isAbandonMessage) return true;
                }
                return false;
            }
        };
    }
     
    private UnaryPredicate predCallAllocateReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isAllocateMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("ALLOCATE_CALL") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isAllocateMessage) return true;
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
                    boolean isPingMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("PING") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isPingMessage) return true;
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
                    boolean isPongMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("PONG") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isPongMessage) return true;
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
    
     //</editor-fold>
    
    //<editor-fold desc="Receivers">
    
    private void receiveData()
    {
        for (Iterator iter = simDataReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            InfoPacket packet = (InfoPacket)relay.getQuery();

            // always remove a relay that has been sent back as a reply.
            blackboard.publishRemove(relay);

            // extract the data needed.
            Object[] data = (Object[])packet.getContent();
            this.handlingTimes = (Vector)data[0];
            this.workingTimes = (Vector)data[1];
            this.callCentre = (String)data[2];                
            this.totalHandlers = Integer.parseInt(data[3].toString());
            this.skillgroup = ((PeriodShiftInfo)workingTimes.get(0)).getSkillgroup();
            
            this.cluster = new Vector();
            this.cluster.add(agentId.getAddress());
            resetPlugin();         
        }
    }
    
    private CallInfo receiveCall()
    {
        CallInfo call = null;
        for (Iterator iter = simCallReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();            
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();            

                call = (CallInfo)packet.getContent();                           
                call.logRoute(agentId.getAddress());
                isLastCall = call.isLastCall();
                updateClock(call.getArrival());
                this.flist = new ForwardList();
            }
        }
        return call;
    }
    
    private CallInfo receiveForwardList()
    {
        CallInfo call = null;
        for (Iterator iter = callForwardReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();            
                
                this.flist = (ForwardList)packet.getContent();            
                call = flist.getCurrentCall();
                call.logRoute(agentId.getAddress());
                isLastCall = call.isLastCall();                
                updateClock(flist.getCurrentTime());
            }
        }
        return call;
    }

    private void receiveAbandonCommand()
    {
        for (Iterator iter = callAbandonReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();                            
                this.flist = (ForwardList)packet.getContent();
                isLastCall = flist.getCurrentCall().isLastCall();                
                updateClock(flist.getCurrentTime());
            }
        }
    }
    
    private void receiveResetCall()
    {
        for (Iterator iter = simResetReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();            
            if (relay != null) {
                resetPlugin();
                
                String agentAddress = cluster.get(0).toString();
                //log.shout("First agent is: " + agentAddress);
                if (isFirstAgent()) {
                    sendMsg(sim, new InfoPacket("NEXT_CALL",null), MessageType.NextCallRequest);
                }
            }
        }
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Utilities">
    
    private void updateClock(Date newTime)
    {
        this.currentTime = newTime;
    }
    
    private void resetPlugin()
    {
        this.nextAvailable = ((PeriodShiftInfo)workingTimes.get(0)).getStart();
        this.shiftEnd = getShiftEnd(0);
        this.currentTime = new Date(0);
        this.callQueue = new Vector();
        this.flist = null;
        this.isLastCall = false;
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
    
    private int getHandlePeriod(CallInfo call)
    {
        return findPeriod(call.getArrival());        
    }
    
    private int findCall(String id)
    {
        for(int i=0; i<callQueue.size(); i++){
            CallInfo call = (CallInfo)callQueue.get(i);
            if (call.getId().compareTo(id) == 0) return i;
        }
        return -1;
    }
    
    private boolean isOnShift()
    {
        return (currentTime.getTime() >= nextAvailable.getTime() && currentTime.getTime() < shiftEnd.getTime());
    }
    
    private boolean isRightCluster(Skill skill)
    {
        for(int x=0; x<skillgroup.getSkillCount(); x++){
            Skill s = skillgroup.getSkillAt(x);
            if (s.getName().compareTo(skill.getName()) == 0) return true;
        }
        return false;
    }
    
    private void removeComponentFile()
    {
        File file = new File(getAgentIdentifier() + ".xml");
        if (file.exists()) {
            file.delete();
        }
    }
    
    private MessageAddress getNextAgent()
    {
       return nextAgent;
    }
    
    private boolean isFirstAgent()
    {
        String agentAddress = cluster.get(0).toString();        
        return (agentAddress.compareTo(agentId.getAddress()) == 0);
    }
    
    private boolean callInQueue(CallInfo c1)
    {
        for(int i=0; i<callQueue.size(); i++){
            CallInfo c2 = (CallInfo)callQueue.get(i);
            if (c1.getId().compareTo(c2.getId()) == 0) return true;
        }
        return false;
    }
    
    private void queueCall(CallInfo call)
    {
        callQueue.add(call);
        Collections.sort(callQueue, new CallComparator());        
    }
    
    private CallInfo getNextCall()
    {        
        CallInfo call = (CallInfo)callQueue.get(0);
        call.setCurrentTime(currentTime);
        return call;
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
    
    private void sendMsg(MessageAddress target,  Object content, MessageType msgType) 
    {    
        // Send a new relay to the target
        SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
        blackboard.publishAdd(relay);
        blackboard.publishRemove(relay);
        //log the message count
        switch(msgType)
        {
            case DataRequest:            
                msgLogger.AddSent();                
                break;
            case LogCall:
                msgLogger.AddSent();            
                //blackboard.publishRemove(relay);
                break;
            case Registered:
                msgLogger.AddSent();
                //blackboard.publishRemove(relay);
                break;
            case NextCallRequest:
                msgLogger.AddSent();            
                //blackboard.publishRemove(relay);
                break;
            case CallForward:
                msgLogger.AddForwardedCall();
                //blackboard.publishRemove(relay);
                break;
        }                               
    }
    
    private void logCall(CallInfo call)
    {        
        sendMsg(logger, new InfoPacket("LOG_CALL", call), MessageType.LogCall);
    }
    
    //</editor-fold>
}
