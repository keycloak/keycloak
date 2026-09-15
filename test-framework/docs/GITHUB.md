# GitHub Actions Job Summary

The test framework has built-in support for creating GitHub Actions job summaries. It will report:

* Failed tests 
* Slow tests

The report is enabled by default when tests are executed on GitHub Actions. If you don't want this report it can be
disabled with the `KC_TEST_GITHUB_ENABLED` environment variable.

Slow test detection can be configured separately by setting the threshold for slow test classes and test methods.
By default, test classes that take longer than 120 seconds and test methods that take longer than 30 seconds are 
reported. This can be configured with `KC_TEST_GITHUB_SLOW_CLASS` and `KC_TEST_GITHUB_SLOW_METHOD` environment 
variables.