This is a really simple Java library for starting, stopping, and controlling
daemon processes written in Java on Unix/Linux.  To allow your program
to run as a daemon, just link this library into your program.
The library does not use native code, nor does it require external
shell scripts.  All OS-dependent functionality is implemented by executing
subprocesses, specifically `sh`, `ps`, and `mkfifo`.

The intended use is to make it easy to build deployable jar files
for server or other long-running applications.  For example, you might
use this library as part of packaging your Java web application into a single
jar file (using embedded [Jetty](http://www.eclipse.org/jetty/)): this library
allows the application to run as a background process, handle runtime
configuration commands, and to shut itself down cleanly.  Once everything
is set up you can then do cool things like the following:

```bash
# Start the application in the background
java -jar myApp.jar start

# Send a command (purgecache) to the running application
# (note that this is just an example: handling non-built-in
# commands is done by logic in your application)
java -jar myApp.jar purgecache

# Start (or restart) the application if it is not
# currently running (or has crashed)
java -jar myApp.jar poke

# Shut down the running application
java -jar myApp.jar shutdown
```

Detailed information can be found at the [wiki](https://github.com/daveho/Daemon/wiki).

The library is open source distributed under the MIT license.

David Hovemeyer <david.hovemeyer@gmail.com>
