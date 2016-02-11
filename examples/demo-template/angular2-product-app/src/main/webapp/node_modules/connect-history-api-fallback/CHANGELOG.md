# Changelog

## Unreleased
 - Rewrite rules are now applied before the request URL is checked for dots.
 - Rewrite rules can be defined as functions to have greater control over the `dot rule`.

## v1.0.0
This version introduces a fair amount of breaking changes. Specifically, instances of the historyApiFallback need to be created via the exported function. Previously, this was not necessary.

 - **Breaking:** Support multiple instances of the historyApiFallback middleware with different configurations.
 - **Breaking:** Loggers are configured per historyApiFallback middleware instance (see `README.md`).
 - The fallback index HTML file can be configured. Default is `/index.html`.
 - Additional rewrite rules can be defined via regular expressions.
