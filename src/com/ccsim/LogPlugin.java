package com.ccsim;

/*
 * LogPlugin.java
 *
 * Created on 23 Jul 2008, 22:53
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.planning.ldm.measure.Conversion;
import org.cougaar.util.UnaryPredicate;

public class LogPlugin extends ComponentPlugin
{    
    private UIDService uids;
    private LoggingService log;
    private Vector logCalls;
    private Vector logMessageCounts;
    private MessageAddress sim;
    private ProjectParameters params;
    
    private IncrementalSubscription simData;
    private IncrementalSubscription simCommand;
    private IncrementalSubscription callDataReceiver;
    private IncrementalSubscription messageCountReceiver;
    
    @Override
    public void load()
    {
        super.load();
                
        log = (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);         
        sim = MessageAddress.getMessageAddress("dtraskas_jvm");        
        uids = (UIDService)getServiceBroker().getService(this, UIDService.class, null);
    }
    
    @Override
    protected void setupSubscriptions() 
    {
        simData = (IncrementalSubscription)blackboard.subscribe(predSimDataReceiver()); 
        simCommand = (IncrementalSubscription)blackboard.subscribe(predSimCommandReceiver()); 
        callDataReceiver = (IncrementalSubscription)blackboard.subscribe(predCallReceiver());
        messageCountReceiver = (IncrementalSubscription)blackboard.subscribe(predMessageCountReceiver());
    }

    @Override
    protected void execute() 
    {               
        if (simData.hasChanged()) receiveData();        
        if (callDataReceiver.hasChanged()) receiveCall();
        
        // To be used with the P2P system.
        if (messageCountReceiver.hasChanged()) receiveMessageCount();
        
        if (simCommand.hasChanged()) {
            saveResults();            
            //saveCalls();        
            //saveMessageCounts();
            log.shout("Finished saving results");
        }        
    }
    
    //<editor-fold desc="Result Processing Functions">
    
    private void saveMessageCounts()
    {
        if (logMessageCounts.size() == 0) return;
        
        try{
            File file = new File("results/messages.csv");
            BufferedWriter wr = new BufferedWriter(new FileWriter(file));
            
            int totalRuns = params.getTotalRuns();
            wr.write("agent_id\ttotal_received\ttotal_sent\tavg_total_received\tavg_total_sent\tavg_calls_received\tavg_calls_forwarded\n");            
            for(int i=0; i<logMessageCounts.size(); i++){
                MessageLogger logger = (MessageLogger)logMessageCounts.get(i);
                
                int totalReceived = logger.getTotalReceived();
                int totalSent = logger.getTotalSent();
                int avgTotalReceived = totalReceived / totalRuns;
                int avgTotalSent = totalSent / totalRuns;
                int callsReceived = logger.getCallsReceived() / totalRuns;
                int callsForwarded = logger.getCallsForwarded() / totalRuns;
                
                String text = logger.getAgentId() + "\t";
                text += totalReceived + "\t";
                text += totalSent + "\t";
                text += avgTotalReceived + "\t";
                text += avgTotalSent + "\t";
                text += callsReceived + "\t";
                text += callsForwarded + "\n";
                
                wr.write(text);
            }               
            wr.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void saveCalls()
    {
        try{
            File file = new File("results/calls.csv");
            BufferedWriter wr = new BufferedWriter(new FileWriter(file));
            DateFormat df = new SimpleDateFormat("HH:mm:ss");
            
            wr.write("handled_id\tcall_id\tarrival\tanswer\thandle\tabandon\thandled_at\tabandoned_at\tnext_available\n");
            
            Calendar cal = new GregorianCalendar();
            String text = "";
            for(int i=0; i<logCalls.size(); i++){
                CallInfo call = (CallInfo)logCalls.get(i);
                
                text = call.getAgentHandlerID() + "\t";
                text += call.getId() + "\t";
                text += df.format(call.getArrival()) + "\t";
                text += call.getAnswerTime() + "\t";
                text += call.getHandleTime() + "\t";
                text += call.getAbandonTime() + "\t";
                
                cal.setTime(call.getArrival());
                cal.add(Calendar.SECOND, (int)(call.getAnswerTime()));
                text += df.format(cal.getTime()) + "\t";
                
                if (call.getAbandonTime() > 0){
                    cal.setTime(call.getArrival());
                    cal.add(Calendar.SECOND, (int)(call.getAbandonTime()));
                    text += df.format(cal.getTime()) + "\t";
                } else {
                    text += "\t";
                }
                
                cal.setTime(call.getArrival());
                cal.add(Calendar.SECOND, (int)(call.getAnswerTime() + call.getHandleTime()));
                text += df.format(cal.getTime()) + "\n";
                        
                wr.write(text);
            }
            wr.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    
    }
    
    private void saveResults()
    {
        try{
            File file = new File("results/results.csv");
            BufferedWriter wr = new BufferedWriter(new FileWriter(file));
            // tsf stands for telephone service factor and is the number of seconds before which
            // a call is concidered within service level. Please note that tsf is used for calls
            // handled and calls abandoned so service level is calculated using both with the following
            // formula:                   handled_tsf
            //            SL =  --------------------------------- x 100%
            //                         handled + abandoned
            //

            Calendar cal = new GregorianCalendar();
            DateFormat df = new SimpleDateFormat("HH:mm:ss");
            
            wr.write("run\tperiod\tgroup\toffered\thandled\thandled_tsf\tabandoned\tabandoned_tsf\ttotal_abandoned\tutilisation\tasa\taht\tqueuelength\tSL%\n");
            
            int totalRuns = params.getTotalRuns();
            Vector groups = params.getGroups();            
            for(int run=1; run<=totalRuns; run++){
                for(int g=0; g<groups.size(); g++){
                    CallCentre group = (CallCentre)groups.get(g);
                    
                    DayShiftInfo fteDay = group.getFTEDayAt(0);
                    cal.setTime(params.getStart());
                    Date dtCurrent = cal.getTime();
                    while(dtCurrent.getTime() < params.getFinish().getTime()){
                        String text = run + "\t";
                        text += df.format(dtCurrent) + "\t";
                        text += group.getName() + "\t";
                        Date prev = dtCurrent;
                        // advance the period by interval    
                        cal.add(Calendar.MINUTE, params.getInterval());
                        dtCurrent = cal.getTime();
                        
                        Object[] outParams = new Object[1];
                        Vector cv = extractCalls(run, group.getName(), prev, dtCurrent, outParams);
                        if (cv != null) {
                            int index = fteDay.findPeriod(prev);
                            PeriodSkillGroupInfo sgi = (PeriodSkillGroupInfo)fteDay.getAt(index);
                            
                            text += processCalls(cv,  Integer.parseInt(outParams[0].toString()), sgi.getTotalFTECount());
                        }
                        text += "\n";                        
                        wr.write(text);
                    }
                }                                                
            }            
            wr.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private String processCalls(Vector cv, int offered, int totalFTE)
    {
        int handled = 0;
        int handled_tsf = 0;
        int abandoned = 0;
        int abandoned_tsf = 0;
        int total_abandoned = 0;
        double utilisation = 0;
        double asa = 0;
        double aht = 0;
        double queuelength = 0;
        double sl = 0;
        double busySeconds = 0;        
        double answerSeconds = 0;
        double patienceSeconds = 0;
        
        for(int x=0; x<cv.size(); x++){
            CallInfo call = (CallInfo)cv.get(x);
            double answerTime = call.getAnswerTime();
            double abandonTime = call.getAbandonTime();
            double handleTime = call.getHandleTime();
            
            int tsf = call.getSkill().getTsf();
            
            if (abandonTime == 0) {
                handled++;
                busySeconds += handleTime;
                answerSeconds += answerTime;                
                
                if (answerTime <= tsf){
                    handled_tsf++;
                }                
            } else {
                total_abandoned++;
                patienceSeconds += abandonTime;
                
                if (abandonTime <= tsf){
                    abandoned_tsf++;
                } else {
                    abandoned++;
                }                                   
            }            
        }
            
        double total = (handled + total_abandoned);
        if (total == 0) {
            sl = 100;
        } else {
            if (total > 0 && handled_tsf > 0) {
                sl = (handled_tsf / total)*100;
            }
        }
        
        if (handled > 0) {
            asa = (answerSeconds / handled);
            aht = (busySeconds / handled);
        } else {
            asa = 0;
            aht = 0;
        }
                
        queuelength = ((answerSeconds + patienceSeconds) / (params.getInterval() * 60));
                
        // calculate utilisation (busy_seconds) / (available_fte * minutes_per_interval * 60) * 100        
        utilisation = (busySeconds / (totalFTE * params.getInterval() * 60)) * 100;
        
        String text = offered + "\t" + handled + "\t" + handled_tsf + "\t" + abandoned + "\t" + 
                      abandoned_tsf + "\t" + total_abandoned + "\t" + utilisation + "\t" + 
                      asa + "\t" + aht + "\t" + queuelength + "\t" + sl;
        return text;
    }
        
    private Vector extractCalls(int run, String group, Date start, Date end, Object[] outParams)
    {
        int offered = 0;
        Vector cv = new Vector();        
        for(int x=0; x<logCalls.size(); x++){
            CallInfo call = (CallInfo)logCalls.get(x);            
            
            double abandonTime = call.getAbandonTime();
            Date checkTime = null;
            if (abandonTime == 0) {
                Calendar cal = new GregorianCalendar();
                cal.setTime(call.getArrival());
                cal.add(Calendar.SECOND, (int)(call.getAnswerTime()));
                checkTime = cal.getTime();
            }else{
                checkTime = call.getCurrentTime();
            }
            
            if ((call.getRun() == run) && call.callCentreExists(group) && 
                (start.getTime() <= checkTime.getTime() && checkTime.getTime() < end.getTime())){
                cv.add(call);
            }
            
            if ((call.getRun() == run) && call.callCentreExists(group) && 
                (start.getTime() <= call.getArrival().getTime() && call.getArrival().getTime() < end.getTime())){                
                offered++;
            }
        }
        outParams[0] = offered;
        return cv;
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Receivers">
    
    private void receiveData()
    {
        for (Iterator iter = simData.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();

                this.params = (ProjectParameters)packet.getContent();            
                this.logCalls = new Vector();
                this.logMessageCounts = new Vector();
            }
        }
    }
    
    private void receiveCall()
    {
        for (Iterator iter = callDataReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();
                CallInfo call = (CallInfo)packet.getContent();
                
                logCalls.add(call);
            }
        }        
    }
    
    private void receiveMessageCount()
    {
        for (Iterator iter = messageCountReceiver.getAddedCollection().iterator(); iter.hasNext();) {
            SimpleRelay relay = (SimpleRelay)iter.next();
            
            if (relay != null){
                InfoPacket packet = (InfoPacket)relay.getQuery();
                MessageLogger logger = (MessageLogger)packet.getContent();

                logMessageCounts.add(logger);
            }
        }
    }
    
    private UnaryPredicate predSimDataReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isLoggerInitMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("LOGGER_INIT") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isLoggerInitMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predSimCommandReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isCommandMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("SAVE_RESULTS") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && sim.equals(relay.getSource()) && isCommandMessage) return true;
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
                    boolean isCallMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("LOG_CALL") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isCallMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predMessageCountReceiver() 
    {    
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay) o;
                    boolean isMessageCountMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("LOG_MESSAGE_COUNT") == 0);
                    
                    if (agentId.equals(relay.getTarget()) && isMessageCountMessage) return true;
                }
                return false;
            }
        };
    }
    
    private void sendMsg(MessageAddress target,  Object content) 
    {
        // Send a new relay to the target
        SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
        blackboard.publishAdd(relay);
        blackboard.publishRemove(relay);
    }
    
    //</editor-fold>
}
