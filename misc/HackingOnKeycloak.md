Hacking on Keycloak
===================

GitHub Repository
-----------------

### Create a GitHub account if you don't already have one

[Join GitHub](https://github.com/join)

### Fork Keycloak repository into your account

[https://github.com/keycloak/keycloak](https://github.com/keycloak/keycloak)

### Clone your newly forked copy onto your local workspace

    git clone https://github.com/<your username>/keycloak.git
    cd keycloak
    
### Add a remote ref to upstream for pulling future updates
    
    git remote add upstream https://github.com/keycloak/keycloak.git
    
### Pull later updates from upstream

    git fetch upstream
    git rebase upstream/master
    
    
Discuss changes
---------------

Before starting work on a new feature or anything besides a minor bug fix join the [Keycloak Dev mailing list](https://lists.jboss.org/mailman/listinfo/keycloak-dev) 
and send a mail about your proposed changes. This is vital as otherwise you may waste days implementing a feature that is later rejected.

Once you have received feedback from the mailing list if there's not one already create a (JIRA issue)[https://issues.jboss.org/browse/KEYCLOAK].
 

Implement changes
-----------------

We don't currently enforce a code style in Keycloak, but a good reference is the code style used by WildFly. This can be retrieved from [Wildfly ide-configs](https://github.com/wildfly/wildfly-core/tree/master/ide-configs).To import formatting rules, see following [instructions](http://community.jboss.org/wiki/ImportFormattingRules)

If your changes requires updates to the database read [Updating Database Schema](UpdatingDatabaseSchema.md).

To try your changes out manually you can quickly start Keycloak from within your IDEA or Maven, to find out how to do this
read [Testsuite](Testsuite.md). It's also important that you add tests to the testsuite for your changes.  
 

Get your changes merged into upstream
-------------------------------------

Here's a quick check list for a good pull request (PR):

* Discussed and agreed on Keycloak Dev mailing list
* One commit per PR
* One feature/change per PR
* No changes to code not directly related to your change (e.g. no formatting changes or refactoring to existing code, if you want to refactor/improve existing code that's a separate discussion to mailing list and JIRA issue)
* A JIRA associated with your PR (include the JIRA issue number in commit comment)
* All tests in testsuite pass
* Do a rebase on upstream master

Once you're happy with your changes go to GitHub and create a PR.
