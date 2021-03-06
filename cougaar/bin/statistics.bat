@echo OFF

REM "<copyright>"
REM " "
REM " Copyright 2004 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects"
REM " Agency (DARPA)."
REM ""
REM " You can redistribute this software and/or modify it under the"
REM " terms of the Cougaar Open Source License as published on the"
REM " Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"
REM " "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"
REM " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"
REM " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"
REM " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
REM " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
REM " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"
REM " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"
REM " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"
REM " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"
REM " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
REM " "
REM "</copyright>"


if [%5] == [] (
  echo Usage: statistics.bat [Host] [database name] [DB Username] [DB Password] [runid]
  GOTO L_END
) 

REM example usage : ./runPercent.bat localhost grabber root "" 221

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" people.sql > peoplerunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < peoplerunid.sql"
REM echo "pax per unit"
mysql -h%1 -u%3 -p%4 %2 < peoplerunid.sql

DEL peoplerunid.sql

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" items.sql > itemsrunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < itemsrunid.sql"
REM echo "items per unit"
mysql -h%1 -u%3 -p%4 %2 < itemsrunid.sql

DEL itemsrunid.sql

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" tons.sql > tonsrunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < tonsrunid.sql"
REM echo "tons per unit"
mysql -h%1 -u%3 -p%4 %2 < tonsrunid.sql

DEL tonsrunid.sql

:L_END
