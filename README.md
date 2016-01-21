Keycloak
========

Open Source Identity and Access Management for modern Applications and Services.

For more information about Keycloak visit [Keycloak homepage](http://keycloak.org) and [Keycloak blog](http://blog.keycloak.org).


Building
--------

Ensure you have JDK 8 (or newer), Maven 3.2.1 (or newer) and Git installed

    java -version
    mvn -version
    git --version
    
First clone the Keycloak repository:
    
    git clone https://github.com/keycloak/keycloak.git
    cd keycloak
    
To build Keycloak run:

    mvn install
    
This will build all modules and run the testsuite. 

To build the distribution run:

    mvn install -Pdistribution
    
Once completed you will find distribution archives in `distribution`.


Starting Keycloak
-----------------

To start Keycloak during development first build as specficied above, then run:

    mvn -f testsuite/integration/pom.xml exec:java -Pkeycloak-server 


To start Keycloak from the appliance distribution first build the distribution it as specified above, then run:

    tar xfz distribution/appliance-dist/target/keycloak-appliance-dist-all-<VERSION>.tar.gz
    cd keycloak-appliance-dist-all-<VERSION>/keycloak
    bin/standalone.sh
    
To stop the server press `Ctrl + C`.


Help and Documentation
----------------------
* [Documentation](http://keycloak.jboss.org/docs) - User Guide, Admin REST API and Javadocs
* [User Mailing List](https://lists.jboss.org/mailman/listinfo/keycloak-user) - Mailing list to ask for help and general questions about Keycloak
* [JIRA](https://issues.jboss.org/projects/KEYCLOAK) - Issue tracker for bugs and feature requests


Contributing
------------

* Developer documentation
    * [Hacking on Keycloak](misc/HackingOnKeycloak.md) - How to become a Keycloak contributor
    * [Testsuite](misc/Testsuite.md) - Details about testsuite, but also how to quickly run Keycloak during development and a few test tools (OTP generation, LDAP server, Mail server)
    * [Database Testing](misc/DatabaseTesting.md) - How to do testing of Keycloak on different databases
    * [Updating Database](misc/UpdatingDatabaseSchema.md) - How to change the Keycloak database
* [Developer Mailing List](https://lists.jboss.org/mailman/listinfo/keycloak-dev) - Mailing list to discuss development of Keycloak


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)