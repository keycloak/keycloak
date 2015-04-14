Keycloak
========

Keycloak is an SSO Service for web apps and REST services. For more information visit [http://keycloak.org](http://keycloak.org).  


Building
--------

Ensure you have JDK 7 (or newer) and Maven 3.2.1 (or newer) installed

    java -version
    mvn -version
    
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


Contributing
------------

* [Hacking On Keycloak](https://github.com/keycloak/keycloak/blob/master/misc/HackingOnKeycloak.md)


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)