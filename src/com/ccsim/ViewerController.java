package com.ccsim;

/*
 * ViewerController.java
 *
 * Created on March 14, 2008, 10:03 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
import org.cougaar.core.mts.MessageAddress;

/**
 *
 * @author dimitriostraskas
 */
public interface ViewerController
{
    static final int START_VIEWER = 0;
    
    public void openModel(String filename);
    
    public void closeModel();
    
    public void deployModel();
    
    public void startSimulation();
    
    public void saveResults();
}
