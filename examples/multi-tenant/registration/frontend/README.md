Registration module - Frontend
=========

This is a simple HTML5 application, build with yo/grunt. You can run it by
executing "grunt serve". Once it executes, it will open a browser window with
the application. You can enter an account name on the first field (this will be
the realm), and admin username (not used at the moment) and an admin password (
also not used).

No changes are needed on this to work. It requires only that the
registration/backend to be deployed and available on localhost:8080 . If you
absolutely need to change the URL for the backend, you can adapt the file
app/scripts/services/environment.coffee . 