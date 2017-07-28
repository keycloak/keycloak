CLI Single Sign On
===================================

This java-based utility is meant for providing Keycloak integration to 
command line applications that are either written in Java or another language.  The
idea is that the Java app provided by this utility performs a login for a specific
client, parses responses, and exports an access token as an environment variable
that can be used by the command line utility you are accessing.

