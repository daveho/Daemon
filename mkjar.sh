rm -f daemon.jar
echo "Main-class: org.cloudcoder.daemon.example.ExampleDaemonController" \
	> manifest.txt
cd bin && jar cfm ../daemon.jar ../manifest.txt org
rm -f manifest.txt
