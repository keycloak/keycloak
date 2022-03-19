Creating Themes
===============

Themes are used to configure the look and feel of login pages and the account management console.

Custom themes packaged in a JAR file should be deployed to the `${kc.home.dir}/providers` directory. After that, run
the `build` command to install them before starting the server.

You are also able to create your custom themes in this directory, directly. Themes within this directory do not require
the `build` command to be installed.

When running the server in development mode using `start-dev`, themes are not cached so that you can easily work on them without a need to restart
the server when making changes.

See the theme section in the [Server Developer Guide](https://www.keycloak.org/docs/latest/server_development/#_themes) for more details about how to create custom themes.

Overriding the built-in templates
---------------------------------

While creating custom themes, especially when overriding templates, it may be useful to use the built-in templates as
a reference. These can be found within the theme directory of `../lib/lib/main/org.keycloak.keycloak-themes-${project.version}.jar`, which can be opened using any
standard ZIP archive tool.

**Built-in themes should not be modified directly, instead a custom theme should be created.**