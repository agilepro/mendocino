:#####################################################################################################
:# 
:# INSTRUCTIONS
:#
:# In order to perform a build, you must edit the values below and make
:# them appropriate for your local build context and your machine.
:# These values will be used by the build script in order to pick up the 
:# resources it needs to complete the build.
:#
:#####################################################################################################


:#####################################################################################################
:#
:# Java home - specify the complete path to the directory where the Java JDK
:# is installed.  For example: JAVA_HOME=e:\Program Files\Java\jdk1.5.0_11\
:#
:#####################################################################################################
set JAVA_HOME=c:\Program Files\Java\jdk1.8.0_131\

:#####################################################################################################
:#
:# Source directory - specify the complete (local) path to the root folder that
:# holds all the source files.  The build will READ from that path, but will
:# not write to that path.
:#
:# Example: SOURCE_DIR=f:\subversion\mendo\
:#
:#####################################################################################################
set SOURCE_DIR=c:\github\mendocino\

:#####################################################################################################
:#
:# Target Directory - specify the complete (local) path to build folder where all
:# of the output will be written.   The final JAR file will be written there, and all
:# temporary intermediate files from the build will be written there.
:#
:# There are two settings:
:# TARGET_DIR is the full path (including drive letter)
:# TARGET_DIR_DRIVE is the drive letter of TARGET_DIR - a kludge till we have a smarter script
:#
:#####################################################################################################
set TARGET_DIR=c:\build\purple\
set TARGET_DIR_DRIVE=C:





:#####################################################################################################
:#
:# NOW test that these settings are correct, test that the folders exist
:# and warn if they do not.  No user settings below here
:#
:#####################################################################################################

IF EXIST "%JAVA_HOME%" goto step2

echo off
echo ************************************************************
echo The Java home folder (%JAVA_HOME%) does not exist.
echo please change JAVA_HOME to a valid folder where Java is installed
echo ************************************************************
pause
echo on
goto exit1

:step2
IF EXIST "%SOURCE_DIR%" goto step3

echo off
echo ************************************************************
echo The source folder (%SOURCE_DIR%) does not exist.
echo please change SOURCE_DIR to a valid folder where source is to be read from
echo ************************************************************
pause
echo on
goto exit1

:step3
IF EXIST "%TARGET_DIR%" goto step4

echo off
echo ************************************************************
echo The build target folder (%TARGET_DIR%) does not exist.
echo please change TARGET_DIR to a valid folder where output is to go
echo ************************************************************
pause
echo on
goto exit1

:step4
echo configuration looks OK

:exit1

