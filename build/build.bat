:#####################################################################################################
:#
:# INSTRUCTIONS
:#
:# In order to perform a build, you must edit the file "build_configuration.bat"
:# to have values appropriate to your system and build environment.
:#
:# "build.bat" is shared by everyone without any changes.
:# There are no settings in this file "build.bat" that require configuration, so
:# you should avoid changing this file in any way to match your unique configuration.
:#
:#####################################################################################################

:### setup
call build_configuration.bat

echo JAVA_HOME is %JAVA_HOME%
echo SOURCE_DIR is %SOURCE_DIR%
echo TARGET_DIR is %TARGET_DIR%
echo TARGET_DIR_DRIVE is %TARGET_DIR_DRIVE%

:### delete any build previously created in TARGET_DIR & recreate folder structure
del /s /q %TARGET_DIR%\mendo.jar
rmdir /s /q %TARGET_DIR%\mendo
mkdir %TARGET_DIR%\mendo

:### setup classpath
set HTMLUNIT_LIB=%TARGET_DIR%\mendo
set HTMLUNIT_CP=%HTMLUNIT_LIB%\commons-codec-1.3.jar;%HTMLUNIT_LIB%\commons-collections-3.2.1.jar;%HTMLUNIT_LIB%\commons-httpclient-3.0.1.jar;%HTMLUNIT_LIB%\commons-io-1.4.jar;%HTMLUNIT_LIB%\commons-lang-2.4.jar;%HTMLUNIT_LIB%\commons-logging-1.1.jar;%HTMLUNIT_LIB%\cssparser-0.9.5.jar;%HTMLUNIT_LIB%\htmlunit-2.3.jar;%HTMLUNIT_LIB%\htmlunit-core-js-2.2.jar;%HTMLUNIT_LIB%\nekohtml-1.9.9.jar;%HTMLUNIT_LIB%\sac-1.3.jar;%HTMLUNIT_LIB%\xalan-2.7.0.jar;%HTMLUNIT_LIB%\xercesImpl-2.8.1.jar;%HTMLUNIT_LIB%\xml-apis-1.0.b2.jar;

set MENDO_CP=%SOURCE_DIR%

:### compile java classes
"%JAVA_HOME%\bin\javac" -source 1.5 -target 1.5 -d %TARGET_DIR%\mendo %SOURCE_DIR%\src\org\workcast\mendocino\*.java %SOURCE_DIR%\src\org\workcast\mendocinotest\*.java %SOURCE_DIR%\src\org\workcast\testframe\*.java %SOURCE_DIR%\src\org\workcast\streams\*.java %SOURCE_DIR%\src\org\workcast\json\*.java

if errorlevel 1 goto EXIT

echo Compile successful

:### build mendo.jar
%TARGET_DIR_DRIVE%
pushd %TARGET_DIR%\mendo
del %TARGET_DIR%\mendo.jar
"%JAVA_HOME%/bin/jar" -cvfM %TARGET_DIR%\mendo.jar *
if errorlevel 1 exit /b

echo mendo.jar created successfully at %TARGET_DIR%

:### restore starting directory
popd

:EXIT
pause
