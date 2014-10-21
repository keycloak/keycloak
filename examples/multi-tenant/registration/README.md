Keycloak Multi Tenants Example - Registration frontend module
=============================================================

This is is a simple AngularJS HTML5 application, bootstrapped using ``Yeoman`` and built via ``grunt``. There's nothing really specific on this application related to multi tenancy. Once you install ``grunt``, you can just execute ``grunt serve`` and a browser window will open with the application. Make sure you have the registration backend started before you proceed with a registration. If you have problems with it, use your browser's developer tools to inspect what it's doing.

Some pointers:

- The backend's base URL is defined on ``app/scripts/services/environment.coffee``
- The main logic is on ``app/scripts/controllers/main.coffee``
- The main view is on ``app/views/main.html``
- ``Registration`` is an AngularJS REST service, defined on ``app/scripts/services/registration.coffee``
