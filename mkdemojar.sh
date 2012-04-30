#! /bin/sh

rm -f demo.jar
echo "Main-class: org.cloudcoder.daemon.example.ExampleDaemonController" \
	> manifest.txt
(cd bin && jar cfm ../demo.jar ../manifest.txt org)
rm -f manifest.txt
