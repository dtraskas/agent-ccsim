package com.ccsim;

/*
 * ACDViewer.java
 *
 * Created on 18 Jan 2008, 23:43
 *
 * Author: Dimitrios Traskas
 * Bath University
 *
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.awt.GridLayout;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 *
 * @author dimitriostraskas
 */
public class ACDViewer extends JPanel
{
    private javax.swing.JScrollPane scrollPanelLeft;
    private javax.swing.JScrollPane scrollPanelRight;
    private javax.swing.JSplitPane splitPanelMain;
    private javax.swing.JTable tblParams;
    private javax.swing.JTextArea txtOutput;
    
    protected ViewerController controller;
    private static Logger logging;
    static {logging = LoggerFactory.getInstance().createLogger(ACDViewer.class);}
    
    public final int ROW_SIM_RUNS = 0;
    public final int ROW_SIM_TTL = ROW_SIM_RUNS + 1;
    public final int ROW_SIM_START = ROW_SIM_TTL + 2;
    public final int ROW_SIM_FINISH = ROW_SIM_START + 1;
    public final int ROW_SIM_INTERVAL = ROW_SIM_FINISH + 1;
    public final int ROW_SIM_PERIODS = ROW_SIM_INTERVAL + 1;
    public final int ROW_SIM_TOTAL_CALLS = ROW_SIM_PERIODS + 1;
    public final int ROW_SIM_TOTAL_HANDLERS = ROW_SIM_TOTAL_CALLS + 1;
    public final int ROW_SIM_CLOCK = ROW_SIM_TOTAL_HANDLERS + 2;
    
    /** Creates a new instance of ACDViewer
     * @param controller 
     */
    public ACDViewer(ViewerController controller)
    {
        super();
        initComponents();
        
        this.controller = controller;
    }
    
    private void initComponents()
    {
        splitPanelMain = new javax.swing.JSplitPane();
        scrollPanelLeft = new javax.swing.JScrollPane();
        scrollPanelRight = new javax.swing.JScrollPane();
        txtOutput = new javax.swing.JTextArea();
        tblParams = new javax.swing.JTable();
        
        splitPanelMain.setDividerLocation(200);
        splitPanelMain.setDividerSize(4);
        scrollPanelLeft.setAutoscrolls(true);
        scrollPanelLeft.setHorizontalScrollBar(null);
        scrollPanelLeft.setMaximumSize(new java.awt.Dimension(150, 150));
        scrollPanelLeft.setPreferredSize(new java.awt.Dimension(50, 100));
        tblParams.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {"Runs", null},
                {"TTL", null},
                {null, null},
                {"Start", null},
                {"Finish", null},
                {"Interval Minutes", null},
                {"Periods", null},
                {"Total Calls", null},
                {"Total Handlers", null},
                {null, null},
                {"Simulation Clock", null},
            },
            new String []
            {
                "Parameter", "Value"
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        tblParams.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblParams.setGridColor(new java.awt.Color(100, 100, 100));
        scrollPanelLeft.setViewportView(tblParams);
        splitPanelMain.setLeftComponent(scrollPanelLeft);
        
        txtOutput.setColumns(20);
        txtOutput.setRows(5);
        txtOutput.setBackground(new java.awt.Color(0, 0, 0));
        txtOutput.setForeground(new java.awt.Color(255, 255, 255));
        txtOutput.setText(">Ready");
        txtOutput.setEditable(false);
        scrollPanelRight.setViewportView(txtOutput);
        splitPanelMain.setRightComponent(scrollPanelRight);
        
        setLayout(new GridLayout());
        add(splitPanelMain);
    }
    
    public JMenuBar createMenuBar()
    {
        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu mnuFile = new javax.swing.JMenu("File");
        javax.swing.JMenuItem mnuFileOpen = new javax.swing.JMenuItem("Open Model", KeyEvent.VK_O);
        javax.swing.JMenuItem mnuFileClose = new javax.swing.JMenuItem("Close Model", KeyEvent.VK_C);
        javax.swing.JSeparator mnuFileSep1 = new javax.swing.JSeparator();
        javax.swing.JMenuItem mnuFileExit = new javax.swing.JMenuItem("Exit", KeyEvent.VK_X);
        javax.swing.JMenu mnuACD = new javax.swing.JMenu("IRN");
        javax.swing.JMenuItem mnuACDDeploy = new javax.swing.JMenuItem("Deploy", KeyEvent.VK_F6);
        javax.swing.JMenuItem mnuACDRunSimulation = new javax.swing.JMenuItem("Run Simulation", KeyEvent.VK_F5);
        javax.swing.JMenuItem mnuACDShutdown = new javax.swing.JMenuItem("Shutdown");
        
        // setting up actions 
        mnuFileOpen.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                openModel();
            }
        });
                
        mnuFileClose.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                closeModel();
            }
        });
        
        mnuFileExit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                exitSystem();
            }
        });
        
        mnuACDDeploy.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                deploy();
            }
        });
        
        
        mnuACDRunSimulation.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                runSim();
            }
        });
        
        
        mnuACDShutdown.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdown();
            }
        });
        
        // adding the menus to the menu bar
        mnuFile.add(mnuFileOpen);
        mnuFile.add(mnuFileClose);
        mnuFile.add(mnuFileSep1);
        mnuFile.add(mnuFileExit);
        menuBar.add(mnuFile);

        mnuACD.add(mnuACDDeploy);
        mnuACD.add(mnuACDRunSimulation);
        mnuACD.add(mnuACDShutdown);
        menuBar.add(mnuACD);
        
        return menuBar;
    }
    
    private void openModel()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Open IRN Model...");
        }
        
        File path = new File(".");
        String initialDirectory = path.getAbsolutePath();
        JFileChooser chooser = new JFileChooser(initialDirectory);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Open Data Folder");
       
        int rc = chooser.showOpenDialog(this);
        if (rc == chooser.APPROVE_OPTION){
            String filename = chooser.getSelectedFile ().getAbsolutePath ();
            controller.openModel(filename);
        }
    }
    
    private void closeModel()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Closing IRN Model...");
        }
        controller.closeModel();
    }
    
    private void exitSystem()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Exit IRN...");
        }
        System.exit(0);
    }
    
    private void deploy()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Deploying IRN Model...");
        }
        controller.deployModel();
    }
    
    private void runSim()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Running simulation...");
        }
        controller.startSimulation();
    }
    
    private void shutdown()
    {
        if (logging.isDebugEnabled()) {
            logging.debug("Shutdown IRN Model...");
        }        
        controller.saveResults();
    }
    
    public JTextArea getOutput()
    {
        return this.txtOutput;
    }
    
    public void setParameter(int param, String value)
    {
        tblParams.getModel().setValueAt(value, param, 1);
    }
    
    public int getRuns()
    {
        return Integer.parseInt(tblParams.getModel().getValueAt(ROW_SIM_RUNS, 1).toString());
    }
    
    public int getTTL()
    {
        return Integer.parseInt(tblParams.getModel().getValueAt(ROW_SIM_TTL, 1).toString());
    }
    
    public void advanceClock(final String time)
    {
        Runnable addMsg = new Runnable()
        {
            public void run()
            {
                tblParams.getModel().setValueAt(time, ROW_SIM_CLOCK, 1);
            }
        };
        SwingUtilities.invokeLater(addMsg);
    }
    
    public void setMessage(final String msg)
    {
        Runnable addMsg = new Runnable()
        {
            public void run()
            {
                txtOutput.append(msg);
            }
        };
        SwingUtilities.invokeLater(addMsg);
    }
}