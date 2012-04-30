This is a really simple Java library for starting, stopping, and controlling
daemon processes written in Java on Unix/Linux.  To allow your program
to run as a daemon, just link this library into your program.
The library does not use native code, nor does it require external
shell scripts.  All OS-dependent functionality is implemented by executing
subprocesses, specifically `sh`, `ps`, and `mkfifo`.

See the `ExampleDaemon` and `ExampleDaemonController` classes as an example
of how to define a daemon.  The `ExampleDaemon` class has methods for
starting the daemon, shutting down the daemon, and receiving commands.
The `ExampleDaemonController` is an example of an entry point class:
for example, if you are doing a single-jar deployment, it would be the
main class.

Running the example daemon: first compile the classes (will happen automatically
if you import the project into Eclipse).  Then cd to the root directory of
the project.  Run the following commands:

```
./mkdemojar.sh
java -jar demo.jar start
java -jar demo.jar hello
java -jar demo.jar shutdown
```

The example shows how
you might package your daemon in a single jar file for deployment.
The last three commands will (respectively) start the daemon, send a command
("hello") to the daemon, and shut down the daemon.
A file called `log.txt` will be created to capture the standard output
and standard error of the daemon.  Each command (start/hello/shutdown) will
be logged in this file.

Some things to note:

* The pid file and the FIFO used to control the running daemon will
  be created in the current directory (the directory the entry point
  `main` method is started from).
* Each running daemon process will have an "instance name".
  A default instance name is used if none is provided when the
  daemon is started.  You can run many daemon processes from the
  same codebase in the same directory as long as they all have
  different instance names.

To build the code as a jar file, build the `jar` target in the
provided `build.xml` using Ant.  This will create a jar file called
`daemon.jar`.

The code is distributed under the MIT license.

Let me know if you have any feedback, patches, etc.

David Hovemeyer <david.hovemeyer@gmail.com>