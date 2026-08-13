# Deprecating the Old Testsuite Module
If you find yourself reading this document, you are probably asked to migrate a test from your PR to use the new Test Framework.

---
With the release of the new Test Framework, this testsuite module, with all related dependents, is officially deprecated.

Specifically speaking: 
* **Arquillian** testsuite
* **Model** testsuite
* All related **utility** modules

A limited amount of changes to existing tests are permitted, and should primarily be used to add test-cases when resolving bugs. Adding new files is not allowed.

## Why Deprecated?
* The Arquillian framework is not a community maintained project anymore. 
* It's using JUnit 4
* The test configuration got messy over the years. 
* Onboarding process has a steep learning curve.
* Adding new features is complex and non-trivial task.

## How to Migrate Your Test?
The new Test Framework brings completely new test configuration and server lifecycle management.
It is tailored to quickly onboard and speed-up the feature development, or contributing a simple bug fix.

Please, follow the new guidelines: 
* [Test Framework](../test-framework/docs/README.md)
* [Migrating Tests](../tests/MANUAL_MIGRATION.md)