# Changing the Default *keycloak-subsystem* Configuration

If you need to make a change to the default keycloak-subsystem 
configuration that is packaged with our distributions, you will need to edit this file:
https://github.com/keycloak/keycloak/blob/master/wildfly/server-subsystem/src/main/config/default-server-subsys-config.properties

This file contains a single multi-line property containing the subsystem 
xml declaration.  Maven filtering is used to read this property and 
inject it everywhere it needs to go.  Editing this file will also take 
care of propagating it to the distributions like server-dist and demo-dist.

Also, you need to create CLI commands for each change by editing this file:
https://github.com/keycloak/keycloak/blob/master/wildfly/server-subsystem/src/main/resources/cli/default-keycloak-subsys-config.cli

This CLI snippet is used in the scripts required by the overlay distribution.

## Updating an SPI
The changes you will likely make are when you need to add a new SPI, change an existing SPI, or add/change a provider within an SPI.

All elements in an SPI declaration are optional, but a full SPI declaration
  looks like this:
````xml
<spi name="example">
     <default-provider>myprovider</default-provider>
     <provider name="myprovider" enabled="true">
         <properties>
             <property name="key" value="value"/>
         </properties>
     </provider>
     <provider name="mypotherrovider" enabled="true">
         <properties>
             <property name="key" value="value2"/>
         </properties>
     </provider>
</spi>
````
Here we have two providers defined for the SPI `example`.  The 
`default-provider` is listed as `myprovider`.  However it is up to the SPI to decide how it will 
treat this setting.  Some SPIs allow more than one provider and some do not.  So
`default-provider` can help the SPI to choose.

Also notice that each provider defines its own set of configuration 
properties.  The fact that both providers above have a property called 
`lockWaitTimeout` is just a coincidence.

## Values of type *List*
The type of each property value is interpreted by the provider. However, 
there is one exception.  Consider the `jpa` provider for the `eventStore` API:
````xml
<spi name="eventsStore">
     <provider name="jpa" enabled="true">
         <properties>
             <property name="exclude-events" value="[&quot;EVENT1&quot;,&quot;EVENT2&quot;]"/>
         </properties>
     </provider>
</spi>
````
We see that the value begins and ends with square brackets.  That means that
the value will be passed to the provider as a list.  In this example, 
the system will pass the
provider a list with two element values `EVENT1` and `EVENT2`. To add 
more values to the list, just separate each list element with a comma. Unfortunately,
you do need to escape the quotes surrounding each list element with 
`&quot;`.
