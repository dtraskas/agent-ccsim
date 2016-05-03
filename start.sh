#!/bin/sh

echo "Configuring IRN..."

export COUGAAR_SOCIETY_PATH=/Users/dimitriostraskas/PhD/experiments/irn/
export COUGAAR_RUNTIME_PATH=/Users/dimitriostraskas/PhD/experiments/irn/run

cp /Users/dimitriostraskas/PhD/experiments/irn/dist/*.jar /Users/dimitriostraskas/PhD/experiments/irn/lib

cougaar -v $COUGAAR_SOCIETY_PATH/config/society.xml $COUGAAR_RUNTIME_PATH/runtime.xml

