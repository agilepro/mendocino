# purple.jar (a.k.a. mendocino)

A collection of useful utility classes for basic JSON, XML, and Streams support.  This was collected by Keith Swenson through implementing many projects that needed the same kinds of classes.  The collection was made open source so that anyone can use it for any Java project.

Find the documentation on line at [PurpleHillsBooks PurpleDoc](http://purplehillsbooks.com/purpleDoc/)

# Approach

The classes are designed to be lightweight and reusable.  This means they do not have a lot of configurable options in order to accomodate dozens of modes of operations. The core capability is provided, and the overhead is minimal.  If you want to extend the classes, then do so with programming, and not thorugh elaborate configurations.  These classes are designed to be used by Java programmers who understand how to use the Java language properly.

# Contents

At a high level, the collection contains:

* JSON support - fast and effective way to parse and generate JSON in Java.  These are lightweight classes that read a JSON file with minimal overhead and represent them as collections in memory.  There is no attempt to automatically transform to other Java classes -- you can do that if you wish with the basic parsing capability provided.  It is really just JSONOBject (the name value association core capability) and JSONArray (for lists of things).  Values are gotten with typed getters, and set with an overloaded setter.  Clear exceptions are reported when anything goes wrong.
* JSON / Exception - note particularly the conversion of an Exception object into a standard JSON represention so that the exception can easily be transmitted in a JSON protocol with a client.
* XML Support - parses and generates XML, and represents in memory as a W3C standard DOM tree.  Originally this was to support data storage in XML files, but today it is abundently clear that you should use JSON for this if possible.  There is some code to convert XML to JSON.
* Streams - can be very powerful if used correctly but Java is missing some convenient functions around streams.  This provides these functions so they don't have to be implemented every time.
