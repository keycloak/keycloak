Keycloak Multi Tenants Example - Business OAuth module
======================================================

Most of what this does is already explained on the parent's module and on the main example's README file. The only code on this module is the ``Runner``. Note that, on this example, this client is an OAuth Client, but could be anything that is able to retrieve an Auth Token, such as an HTML5 client, a mobile application or a third-party application.

The ``Runner`` needs to be customized before you can execute it. In the real world, following the idea of our sample application, this would be an agent downloaded by an user and would be already customized for this client. To keep this example simple and focused, you just need to run the Registration first and change the constants inside the Runner with the credentials obtained from that.
