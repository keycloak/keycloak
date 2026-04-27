# Test stability utils

This directory contains a number of utils to schedule many runs on GitHub Actions and parsing the results. This is 
useful to periodically do a sanity check of the test stability.

Some dedicated GitHub Actions workflows are used for this purpose, as other workflows include test retries, and will
also cancel multiple concurrent runs for the same workflow.

Available workflows:

* stability-base.yml
* stability-clustering.yml
* stability-js-ci.yml

There is also another special workflow `stability-base-reruns.yml` this can be used to run a single test within the
base testsuite many times, which can be useful when trying to fix instability in a specific test.

The runs should not be scheduled in the main Keycloak organization, but rather in your own fork of Keycloak. This is to
prevent impacting other developers and contributors.

To schedule a run use `schedule-runs.sh`, for example:

```
./schedule-runs.sh -w stability-base.yml
```

Once scheduled you need to wait for all runs to complete, you can check the status with `status.sh`, for example:

```
./status.sh -w stability-base.yml
```

After all runs have completed you can download the logs for the failed runs using `download-logs.sh`, for example:

```
./download-logs.sh -w stability-base.yml
```

Final step is to parse the logs to get a report of failed tests using `parse-logs.sh`, for example:

```
./parse-logs.sh logs
```
