
call build_configuration.bat

:##################################################################################
:##### Setup classpath
:##################################################################################

set MENDO_CP=%TARGET_DIR%\purple.jar
set MENDO_TEST_OUT=%TARGET_DIR%\testoutput
rmdir /s /q %MENDO_TEST_OUT%
mkdir %MENDO_TEST_OUT%

:##################################################################################
:##### Run tests
:##################################################################################
"%JAVA_HOME%/bin/java" -classpath %MENDO_CP% com.purplehillsbooks.testcase.JSONTest %SOURCE_DIR% %MENDO_TEST_OUT%
"%JAVA_HOME%/bin/java" -classpath %MENDO_CP% com.purplehillsbooks.testcase.MemFileTester %SOURCE_DIR% %MENDO_TEST_OUT%
"%JAVA_HOME%/bin/java" -classpath %MENDO_CP% com.purplehillsbooks.testcase.Test2 %SOURCE_DIR% %MENDO_TEST_OUT%
"%JAVA_HOME%/bin/java" -classpath %MENDO_CP% com.purplehillsbooks.testcase.TestTemplates %SOURCE_DIR% %MENDO_TEST_OUT%
"%JAVA_HOME%/bin/java" -classpath %MENDO_CP% com.purplehillsbooks.testcase.Test1 %SOURCE_DIR% %MENDO_TEST_OUT%

pause
