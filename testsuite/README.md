Executing testsuite
===================

Currently the testsuite is not executed as part of a regular build. To run the testsuite first you need to do a "mvn clean install" on the project. Then you need to install JBoss AS 7.1.1.Final and upgrade Resteasy (see descriptions in examples/as7-eap-demo/README.md).

The tests can either be run in remote (JBoss AS already running) or managed (tests starts/stops JBoss AS).

To run tests in remote mode:

    mvn clean install -Pjboss-remote

To run tests in managed mode:

    export JBOSS_HOME=<path to JBoss AS 7.1.1.Final with upgraded Resteasy>
    mvn clean install -Pjboss-managed
    
When running tests in the testsuite from an IDE it is best to use the remote mode.

Browser
-------

The testsuite uses Arquillian Drone and Graphene. By default it uses the headless PhantomJS webkit, but it is also possible to run it with other browsers. For example using Firefox or Chrome is good if you want to step-through a test to see what's actually going in.

To run the tests with Firefox add `-Dbrowser=firefox` or for Chrome add `-Dbrowser=chrome`