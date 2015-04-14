So you are a developer who wants to start hacking on Keycloak?  Here is the short list of things you need to know:

1. You'll get a good feel for the Keycloak server and adapters if you try out the demo apps.  Instructions for setting that up are at [https://github.com/keycloak/keycloak/tree/master/examples/demo-template](https://github.com/keycloak/keycloak/tree/master/examples/demo-template).
2. The build has three Maven roots.  There is the obvious one at the root of the project, which builds all the core stuff.  The second one is in /distribution.  That assembles the appliance, the adapters, and a few other things.  The third is in /docbook.  That one creates the documentation.
3. We track everything in [Jira](https://issues.jboss.org/browse/KEYCLOAK).  Make sure you create an issue for any changes you propose.
4. We work with GitHub in much the same way as the WildFly project.  You can look at [Hacking on Wildfly](https://developer.jboss.org/wiki/HackingOnWildFly) to get some tips on that.
5. If you have other questions, ask on the [Developer Mailing List](https://lists.jboss.org/mailman/listinfo/keycloak-dev).  We don't use IRC much, so that's the best place to ask.
6. For a more productive development, please consider using org.keycloak.testutils.KeycloakServer. This class is a Java Application that starts a KC server without requiring you to deploy a WAR file in a specific container.
  