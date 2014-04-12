This is a really simple Java library for starting, stopping, and controlling
daemon processes written in Java on Unix/Linux.  To allow your program
to run as a daemon, just link this library into your program.
The library does not use native code, nor does it require external
shell scripts.  All OS-dependent functionality is implemented by executing
subprocesses, specifically `sh`, `ps`, and `mkfifo`.

Detailed information can be found at the [wiki](https://github.com/daveho/Daemon/wiki).

The library is open source distributed under the MIT license.

David Hovemeyer <david.hovemeyer@gmail.com>