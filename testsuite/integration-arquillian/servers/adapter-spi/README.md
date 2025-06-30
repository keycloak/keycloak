# Adapter SPI

This module is primarily used for custom adapters in the testsuite.

## Undertow
Modules related to Undertow:
* Keycloak Undertow Adapter SPI (`undertow-adapter-spi-jakarta`)
* Keycloak Undertow SAML adapter (`undertow-adapter-saml-jakarta`)

These modules are automatically generated from the Keycloak adapters module (`/adapters`) and converted to adapters supporting JakartaEE.

You can override files from the original module by putting the class with the same name into the `src` directory.
Do not forget to edit `.gitignore` file for changes, which should be kept in the module.

You can check the behavior in `undertow-adapter-spi-jakarta` module and class `UndertowHttpServletRequest`.
