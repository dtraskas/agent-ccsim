package com.ccsim;

/*
 * ACDViewerPlugin.java
 *
 * Created on 14 Jul 2008, 21:51
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import javax.swing.JFrame;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;


/**
 *
 * @author dimitriostraskas
 */
public class ACDViewerPlugin extends ComponentPlugin implements ViewerController
{
    private ACDViewer viewerPanel;
    
    // simulation specific variables
    private ACDProject project;
    private HashMap handlerData;    
    private int totalHandlers;
    private int handlersRegistered;
    private boolean canStartSimulation;

    private DateFormat df;
    private Vector callQueue;
    private int endTick;
    private int currentTick;
    private int totalRuns;
    private int currentRun;
    private int TTL;
    private HashMap handlersPerCluster;
    private int repliesPerCluster;        
    
    // Cougaar specific variables
    private UIDService uids;
    private LoggingService log;
    protected MobilityFactory mobilityFactory;
    private DomainService domain;
    private MessageAddress logger;
    
    // subscriptions
    private IncrementalSubscription dataRelays;
    private IncrementalSubscription registrationRelays;
    private IncrementalSubscription simRelays;
    
    private Random rnd;
            
    public void setDomainService(DomainService domain) 
    {
        this.domain = domain;
        mobilityFactory = (MobilityFactory) domain.getFactory("mobility");        
    }
    
    @Override
    public void load()
    {        
        super.load();                      
        this.rnd = new Random();
               
        df = new SimpleDateFormat("HH:mm:ss");
        uids = (UIDService)getServiceBroker().getService(this, UIDService.class, null);
        log = (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);         
        logger = MessageAddress.getMessageAddress("logger");
        resetAgent();                
    }
    
    /**
     * SetupSubscriptions is called when the Plugin is loaded.
     */
    protected void setupSubscriptions()
    {
        createGUI();
        
        // check the mobility factory exists
        if (mobilityFactory == null) viewerPanel.getOutput().setText("Mobility factory (and domain) not enabled");        
        // receives data requests from the Agent Handlers
        dataRelays = (IncrementalSubscription)blackboard.subscribe(predDataReceiver());        
        // receives registration notifications from Agent Handlers
        registrationRelays = (IncrementalSubscription)blackboard.subscribe(predRegistrationReceiver());        
        // listens for replies with call allocations.        
        simRelays = (IncrementalSubscription)blackboard.subscribe(predRelaySender());             
    }
    
    protected void execute()
    {
        if (canStartSimulation) {
             // check if the simulation is finished
            if (currentRun <= totalRuns) {                
                if (currentTick < endTick) {
                   // start listening to the call allocation messages
                   if (simRelays.hasChanged()){                   
                       for (Iterator iter = simRelays.getAddedCollection().iterator(); iter.hasNext();){    
                            SimpleRelay relay = (SimpleRelay) iter.next();
                            repliesPerCluster++;
                                    
                            //log.shout("Received request from " + relay.getSource());
                            if (repliesPerCluster == handlersPerCluster.size()){        
                                CallInfo currentCall = (CallInfo)callQueue.get(currentTick++);
                                currentCall.setRun(currentRun);
                                //currentCall.setTTL(TTL);
                                if (currentTick == endTick) currentCall.setLastCall();

                                //log.shout("Next call at: " + df.format(currentCall.getArrival()));
                                // pick randomly a handling agent.                        
                                //MessageAddress handler = MessageAddress.getMessageAddress(getHandlerId());  
                                for(int i=0; i<handlersPerCluster.size(); i++){                                    
                                    MessageAddress handler = MessageAddress.getMessageAddress(handlersPerCluster.values().toArray()[i].toString());  
                                    sendMsg(handler, new InfoPacket("NEW_CALL", currentCall));
                                }
                                repliesPerCluster = 0;
                                viewerPanel.advanceClock(df.format(currentCall.getArrival()));
                            }
                       }
                    }                
                } else {           
                    currentRun++;
                    if (currentRun <= totalRuns) {
                        initialiseRun(currentRun);
                    } else {
                        sendNotifyToAllHandlers();                        
                        viewerPanel.getOutput().setText(">Simulation completed succesfully.");
                    }
                }
            }
        } else {                        
            // Observe added relays by looking at our subscription's add list
            for (Iterator iter = dataRelays.getAddedCollection().iterator(); iter.hasNext();) {
                SimpleRelay relay = (SimpleRelay) iter.next();
                blackboard.publishRemove(relay);
                
                //log.shout("Handlers in cluster " + handlersPerCluster.size());
                String handlerId = relay.getSource().getAddress();                
                sendMsg(relay.getSource(),new InfoPacket("DATA_SEND", getData(handlerId)));
            }    

            // Observe agents that have gone through the bootstrapping process.
            for (Iterator iter = registrationRelays.getAddedCollection().iterator(); iter.hasNext();) {
                SimpleRelay relay = (SimpleRelay) iter.next();
                InfoPacket packet = (InfoPacket)relay.getQuery();
                        
                if (packet.getContent() != null){
                    SkillGroup sg = (SkillGroup)packet.getContent();
                    if (!handlersPerCluster.containsKey(sg.getName())){
                        handlersPerCluster.put(sg.getName(), relay.getSource().getAddress());
                    }        
                }
                handlersRegistered++;
            }
            
            // wait until all Agents go through the bootstrapping sequence.
            if (handlersRegistered == totalHandlers) {
                canStartSimulation = true;
                
                // remove the data messages listener
                blackboard.unsubscribe(dataRelays);                                
                viewerPanel.getOutput().append(">Agent Network deployed.");
            }
        }
    }

    //<editor-fold desc="Main Functions">
    
    private void generateHandlers()
    {
        String text = "<agent class=\"org.cougaar.core.agent.SimpleAgent\" name=\"HA_\">\n";
               //text += "<component class=\"HandlerPlugin\" insertionpoint=\"Node.AgentManager.Agent.PluginManager.Plugin\" priority=\"COMPONENT\"/>\n";
               //text += "<component class=\"HandlerPlugin_Exp1\" insertionpoint=\"Node.AgentManager.Agent.PluginManager.Plugin\" priority=\"COMPONENT\"/>\n";
               //text += "<component class=\"HandlerPlugin_Exp2\" insertionpoint=\"Node.AgentManager.Agent.PluginManager.Plugin\" priority=\"COMPONENT\"/>\n";               
               //text += "<component class=\"org.cougaar.planning.servlet.PlanViewServlet\"><argument>/tasks</argument></component>";
               text += "<component class=\"com.dtraskas.irn.SGHandlerPlugin\" insertionpoint=\"Node.AgentManager.Agent.PluginManager.Plugin\" priority=\"COMPONENT\"/>\n";
               text += "</agent>\n";
        
        int totalCount = 1;
        String newText = "";        
        for(Enumeration e = project.getCallCentres().elements(); e.hasMoreElements ();){
            CallCentre cc = (CallCentre)e.nextElement();
            for(int i=0; i<cc.getHandlerCount(); i++){
                CallHandler ch = (CallHandler)cc.getAt(i);
                Object[] data = new Object[4];
                data[0] = ch.getHandleTimes();
                data[1] = ch.getShifts();
                data[2] = cc.getName();
                data[3] = totalHandlers;                
                
                String key = "HA_" + totalCount++;
                handlerData.put(key, data);               
                newText = text.replaceAll("HA_", key);
                createXMLFile(key, newText);
                addAgent(key, "dtraskas_jvm");
            }
        }
    }
    
    private void pingAllHandlers()
    {
        for(int i=1; i<=totalHandlers; i++){
            MessageAddress handler = MessageAddress.getMessageAddress("HA_" + i);      
            blackboard.openTransaction();
            sendMsg(handler, new InfoPacket("START_BOOTSTRAP", null));
            blackboard.closeTransaction();
        }
    }
    
    private void resetHandlers()
    {
        //log.shout("reset handlers");
        for(int i=1; i<=totalHandlers; i++){
            MessageAddress handler = MessageAddress.getMessageAddress("HA_" + i);      
            sendMsg(handler, new InfoPacket("RESET", null));
        }        
    }
    
    private void createXMLFile(String name, String text)
    {
        File file = new File(name + ".xml");
        BufferedWriter wr = null;
        try {
            wr = new BufferedWriter(new FileWriter(file));
            wr.write(text);
            wr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }        
    
    private Object getData(String handlerId)
    {
        if (handlerData.containsKey(handlerId)) {
            return handlerData.get(handlerId);
        }
        return null;
    }
    
    private String getHandlerId()
    {
        int index = rnd.nextInt(totalHandlers) + 1;
        return "HA_" + index;
    }
    
    private void sendNotifyToAllHandlers()
    {
        for(int i=1; i<=totalHandlers; i++){
            MessageAddress handler = MessageAddress.getMessageAddress("HA_" + i);      
            sendMsg(handler, new InfoPacket("LOG_MESSAGE_COUNT", null));
        }
    }
    
    //</editor-fold>
    
    //<editor-fold desc="GUI Functions">
            
    private void createGUI() 
    {
        if (viewerPanel == null) {
            viewerPanel = new ACDViewer(this);

            JFrame frame = new JFrame("IRN Simulator");
            frame.getContentPane().add(viewerPanel);
            frame.setJMenuBar(viewerPanel.createMenuBar());
            frame.pack();
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }
    
    public void openModel(String filename)
    {
        resetParameters();
        
        project = new ACDProject();
        project.load(filename);        
        project.generate();
        
        totalHandlers = project.getTotalHandlers();
        canStartSimulation = false;
        handlersRegistered = 0;
        callQueue = project.getGlobalQueue();
        
        // self-explanatory just generates the handlers
        generateHandlers();
        viewerPanel.getOutput().setText(">Agent Network generated succesfully\n"); 
        
        // set the parameters on the GUI        
        viewerPanel.setParameter(viewerPanel.ROW_SIM_RUNS, "1");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_START, project.getSimStartString());
        viewerPanel.setParameter(viewerPanel.ROW_SIM_FINISH, project.getSimFinishString());
        viewerPanel.setParameter(viewerPanel.ROW_SIM_INTERVAL, String.valueOf(project.getDefaultInterval()));
        viewerPanel.setParameter(viewerPanel.ROW_SIM_PERIODS, String.valueOf(project.getPeriods()));
        viewerPanel.setParameter(viewerPanel.ROW_SIM_TOTAL_CALLS, String.valueOf(project.getTotalCalls()));
        viewerPanel.setParameter(viewerPanel.ROW_SIM_TOTAL_HANDLERS, String.valueOf(totalHandlers));
        
        viewerPanel.setParameter(viewerPanel.ROW_SIM_CLOCK, project.getSimStartString());
    }

    public void closeModel()
    {
        // first kill all the handler agents and reset the simulation agent
        
        // then unload the current project and reset the parameters
        project.unload();        
        resetAgent();
        resetParameters();
    }

    public void deployModel()
    {
        pingAllHandlers();
        viewerPanel.getOutput().append(">Agent Network being deployed...\n"); 
    }
    
    public void startSimulation()
    {
        if (canStartSimulation) {
            viewerPanel.getOutput().setText(">Initialising Simulation...");
            
            totalRuns = viewerPanel.getRuns();
            //TTL = viewerPanel.getTTL();
            ProjectParameters params = project.getParams();
            params.setTotalRuns(totalRuns);
            
            blackboard.openTransaction();
            sendMsg(logger, new InfoPacket("LOGGER_INIT", params));
            blackboard.closeTransaction();
            
            currentRun = 1;            
            viewerPanel.getOutput().setText(">Running Simulation Run " + currentRun);
            endTick = callQueue.size();
            currentTick = 0;

            // extract the first call in the queue and advance the clock
            CallInfo currentCall = (CallInfo)callQueue.get(currentTick++);
            currentCall.setRun(currentRun);
            //currentCall.setTTL(TTL);
            //log.shout("Next call at: " + currentCall.getArrival());
            //pick randomly a call handling agent.
            //MessageAddress handler = MessageAddress.getMessageAddress(getHandlerId());            
            repliesPerCluster = 0;
            for(int i=0; i<handlersPerCluster.size(); i++){
                MessageAddress handler = MessageAddress.getMessageAddress(handlersPerCluster.values().toArray()[i].toString());  
                blackboard.openTransaction();
                sendMsg(handler, new InfoPacket("NEW_CALL", currentCall));
                blackboard.closeTransaction();
            }            
            viewerPanel.advanceClock(df.format(currentCall.getArrival()));
        }
    }
    
    private void initialiseRun(int run)
    {
        viewerPanel.getOutput().setText(">Running Simulation Run " + run);
        endTick = callQueue.size();
        currentTick = 0;
        project.generateCalls();
        callQueue = project.getGlobalQueue();
        
        //reset all handlers
        resetHandlers();
        repliesPerCluster = 0;
        //log.shout("Initialise run " + run);
        /*
        // extract the first call in the queue and advance the clock
        CallInfo currentCall = (CallInfo)callQueue.get(currentTick++);
        currentCall.setRun(currentRun);
        //currentCall.setTTL(TTL);
        
        //log.shout("Next call at: " + currentCall.getArrival());
        repliesPerCluster = 0;
        for(int i=0; i<handlersPerCluster.size(); i++){
            MessageAddress handler = MessageAddress.getMessageAddress(handlersPerCluster.values().toArray()[i].toString());
            sendMsg(handler, new InfoPacket("NEW_CALL", currentCall));
        }
        viewerPanel.advanceClock(df.format(currentCall.getArrival()));
         */
    }
    
    public void saveResults()
    {
        blackboard.openTransaction();            
        sendMsg(logger, new InfoPacket("SAVE_RESULTS", null));
        blackboard.closeTransaction();
    }
    
    private void resetParameters()
    {
        // reset the parameters on the GUI
        viewerPanel.setParameter(viewerPanel.ROW_SIM_RUNS, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_START, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_FINISH, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_INTERVAL, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_PERIODS, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_TOTAL_CALLS, "");
        viewerPanel.setParameter(viewerPanel.ROW_SIM_TOTAL_HANDLERS, "");
        
        viewerPanel.setParameter(viewerPanel.ROW_SIM_CLOCK, "");
        viewerPanel.getOutput().setText(">Ready");
    }
    
    private void resetAgent()
    {
        handlerData = new HashMap();
        handlersPerCluster = new HashMap();
        totalHandlers = -1;
        totalRuns = 1;
        canStartSimulation = false;
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Cougaar Specific Functions">    
    
    private UnaryPredicate predDataReceiver() 
    {
        // Matches any relay sent to our agent
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                boolean isRelay = (o instanceof SimpleRelay);
                boolean isAddressedToSim = false;
                boolean isDataMessage = false;
                if (isRelay) {
                    SimpleRelay relay = (SimpleRelay)o; 
                    isAddressedToSim = agentId.equals(relay.getTarget());
                    isDataMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("DATA_REQUEST") == 0);                    
                }                
                return (isRelay && isAddressedToSim && isDataMessage);
            }
        };
    }
    
    private UnaryPredicate predRegistrationReceiver() 
    {
        // Matches any relay sent to our agent
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                boolean isRelay = (o instanceof SimpleRelay);
                boolean isAddressedToSim = false;
                boolean isRegisteredMessage = false;
                if (isRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                    
                    isAddressedToSim = agentId.equals(relay.getTarget());
                    isRegisteredMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("BOOTSTRAP_COMPLETE") == 0);
                }
                
                return (isRelay && isAddressedToSim && isRegisteredMessage);
            }
        };
    }
    
    private UnaryPredicate predRelaySender() 
    {
        // Matches any relay sent to our agent
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isNextCallMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("NEXT_CALL") == 0);
                    if (agentId.equals(relay.getTarget()) && isNextCallMessage) return true;
                }
                return false;
            }
        };
    }
    
    private UnaryPredicate predResetSender() 
    {
        // Matches any relay sent to our agent
        return new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof SimpleRelay) {
                    SimpleRelay relay = (SimpleRelay)o;                
                    boolean isResetCompleteMessage = (((InfoPacket)relay.getQuery()).getSubject().compareTo("RESET_COMPLETE") == 0);
                    if (agentId.equals(relay.getTarget()) && isResetCompleteMessage) return true;
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
    
    
    private void replyTo(SimpleRelay relay, Object content) 
    {
        // Send back the same content as our response
        relay.setReply(content);         
        blackboard.publishChange(relay);
    }
    
    
    protected void addAgent(String newAgent, String destNode) 
    {
        MessageAddress newAgentAddress = null;
        MessageAddress destNodeAddress = null;
        
        if (newAgent != null) newAgentAddress = MessageAddress.getMessageAddress(newAgent);
        if (destNode != null) destNodeAddress = MessageAddress.getMessageAddress(destNode);
        
        Object ticketId =  mobilityFactory.createTicketIdentifier();
        AddTicket addTicket = new AddTicket(ticketId, newAgentAddress, destNodeAddress);
        AgentControl ac = mobilityFactory.createAgentControl(null, destNodeAddress, addTicket);
        
        blackboard.openTransaction();
        blackboard.publishAdd(ac);
        blackboard.closeTransaction();
    }
    //</editor-fold>
}

