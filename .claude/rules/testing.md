---
paths:
  - "testsuite/integration-arquillian/tests/base/**"
  - "tests/base/**"
---

# Rules

- Always write a test for a fix. If you do not know how to create the test ask for help.
- The arquillian test suite module, also known as old testsuite, is located at the base path `testsuite/integration-arquillian`.
- The test suite module, also known as new test suite, is located at the base path `tests/base`.
- To run a specific test in the test suite, use `./mvnw -f <test-module-path>/pom.xml clean install -Dtest=<test-class-name>`
- To run a specific test in the old test suite, use `./mvnw -f <test-module-path>/pom.xml -Pauth-server-quarkus clean install -Dtest=<test-class-name>`