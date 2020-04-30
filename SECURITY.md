# Security Policy

## Supported Versions

To receive fixes for security vulnerabilities it is required to always upgrade to the latest version of Keycloak. 
See https://www.keycloak.org/downloads for the latest release.

Fixes will only be released for previous releases under special circumstances.

## Reporting a Vulnerability

To report a security vulnerability:

You can report a security vulnerability either through email, or in our issue tracker. If you are uncertain what you have 
discovered is a vulnerability or you believe it is a critical issue please report using email (or both).

To report through email send an email to keycloak-security@googlegroups.com. 

To report through issue tracker:

* Go to https://issues.jboss.org/browse/KEYCLOAK
* Create a new issue in the Keycloak project
* Make sure the "This issue is security relevant" checkbox is checked

If you have a patch for the issue please use `git format-patch` and attach to the email or issue. Please do not open a 
pull request on GitHub as that may disclose sensitive details around the vulnerability.
