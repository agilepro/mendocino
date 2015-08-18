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
set JAVA_HOME=E:\Program Files\Java\jdk1.5.0_11\

:#####################################################################################################
:#
:# Source directory - specify the complete (local) path to the root folder that
:# holds all the source files.  The build will READ from that path, but will
:# not write to that path.
:#
:# Example: SOURCE_DIR=f:\subversion\mendo\
:#
:#####################################################################################################
set SOURCE_DIR=F:\subversion\mendo\

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
set TARGET_DIR=F:\sandbuild\mendo\
set TARGET_DIR_DRIVE=F:

