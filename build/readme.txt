HOW TO BUILD


--------------------------------------------------------------------------
PREREQUISITES

Before you can build the Mendocino library, you will need to 
have a Java JDK installed.  That is where the Java compiler
is that is used to do the build.

That is the only requirement.


--------------------------------------------------------------------------
SETUP

(1) You must create a build folder on a disk that you can write
to and that all the output of the build to be stored.   Create the folder
anywhere, with any name, but remember the full path and use that path
anytime these instructions use the term <buildfolder>.

It is worth noting that the build will write only to this folder and 
to folders under this folder.  It will NEVER write any output to the source
directories, so you don't have to worry about your source folders
becoming corrupted, or unecessarily changed by the build.  THis allows
the source to be on a read-only device like a CD-ROM, or on a read-only
network drive, without preventing the ability to make a build.  It is also
easy to archive the results of a build separately from the source.


(2) Copy the contents of the build-source directory.  This is a directory
within the managed source called "build".  It contains a couple of .bat 
files.


(3) Edit the "build_configuration.bat" to enter the values there.  
Read the instructions in that file on exactly what values need to be entered
there -- it is only a few critical paths to where things are installed
and where the output should be written.

The "build_configuration.bat" is the only file that needs to be modified for
your particular system.  The rest of the build uses this file for settings.


--------------------------------------------------------------------------
RUNNING THE BUILD


All you have to do on Windows is to run "build.bat".    We don't at this time
have build scripts for other operating systems.


--------------------------------------------------------------------------
RUNNING THE TESTS

As every good agile developer knows, the first thing you should do after 
building a library, is to run the tests and make sure that existings tests 
were not broken by recent changes.  This is done by executing "test.bat"



--------------------------------------------------------------------------
