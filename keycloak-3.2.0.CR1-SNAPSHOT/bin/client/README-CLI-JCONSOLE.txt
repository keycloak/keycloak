The jboss-cli-client jar can be used to remotely manage a WildFly instance with CLI or jconsole.  Copy jboss-cli-client.jar to your client machine.  You do not need to install WildFly.

TO RUN CLI:
java -jar <PATH TO jboss-cli-client.jar> [--help] [--version] [--controller=host:port]
                                         [--gui] [--connect] [--file=file_path]
                                         [--commands=command_or_operation1,command_or_operation2...]
                                         [--command=command_or_operation]
                                         [--user=username --password=password]

Use --help for an explanation of the CLI command line options.


TO RUN JCONSOLE:
jconsole -J-Djava.class.path=<PATH TO jconsole.jar>;<PATH TO tools.jar>;<PATH TO jboss-cli-client.jar>

Path to jconsole.jar and tools.jar is typically <JAVA_HOME>/lib/jconsole.jar and <JAVA_HOME>/lib/tools.jar.