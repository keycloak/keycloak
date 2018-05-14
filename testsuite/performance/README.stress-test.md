# Stress Testing

Stress testing is a type of performance testing focused on *finding the maximum performance* of the system for a specific scenario.

There are various strategies but in general the stress test is a cycle of individual tests runs.
After each run the performance assertions are evaluated before deciding if/how the loop should continue.

The [test assertions](https://gatling.io/docs/2.3/general/assertions/) are constructed as boolean expressions on top of computed performance metrics, such as mean response time, percentage of failed requests, etc.


## Requirements

- `bc` tool for floating-point arithmetic


## Usage

`./stress-test.sh [ADDITIONAL_TEST_PARAMS]`

Parameters of the stress test are loaded from `stress-test-config.sh`.

Additional `PROVISIONING_PARAMETERS` can be set via environment variable.

## Common Parameters

| Environment Variable | Description | Default Value |
| --- | --- | --- | 
| `algorithm` | Stress test loop algorithm. Available values: `incremental`, `bisection`. | `incremental`  |
| `provisioning` | When `true` (enabled), the `provision` and `import-dump` operations are run before, and the `teardown` operation is run after test in each iteration. Warm-up is applied in all iterations. When `false` (disabled), there is no provisioning or teardown, and the warm-up is only applied in the first iteration. | `true` (enabled) |
| `PROVISIONING_PARAMETERS` | Additional set of parameters passed to the provisioning command. | |
| `maxIterations` | Maximum number of iterations of the stress test loop. | `10` iterations |
| `dataset` | Dataset to be used. | `100u2c`  |
| `warmUpPeriod` | Sets value of `warmUpPeriod` parameter. If `provisioning` is disabled the warm-up is only done in the first iteration. | `120` seconds  |
| `sequentialUsersFrom` | Value for the `sequentialUsersFrom` test parameter. If provisioning is disabled the value passed to the test command will be multiplied with each iteration. To be used with registration test scenario. | `-1` (random user iteration) |


## Incremental Method

Incremental stress test is a loop with gradually increasing load being put on the system.
The cycle breaks with the first loop that fails the performance assertions, or after a maximum number of iterations

It is useful for testing how various performance metrics evolve dependning on linear increments of load.

### Parameters of Incremental Stress Test

| Environment Variable | Description | Default Value |
| --- | --- | --- | 
| `usersPerSec0` | Value of `usersPerSec` parameter for the first iteration. | `5` user per second |
| `incrementFactor` | Factor of increment of `usersPerSec` with each subsequent iteration. The `usersPerSec` for iteration `i` (counted from 0) is computed as `usersPerSec0 + i * incrementFactor`. | `1` |


## Bisection Method

This method (also called interval halving method) halves an interval defined by the lowest and highest expected value.
The test is performed with a load value from the middle of the specified interval and depending on the result either the lower or the upper half is used in the next iteration.
The cycle breaks when the interval gets smaller than a specified tolerance value, or after a maximum number of iterations.

If set up properly the bisection algorithm is typically faster and more precise than the incremental method.
However it doesn't show metrics evolving with the linear progression of load.

### Parameters of Bisection Stress Test

| Environment Variable | Description | Default Value |
| --- | --- | --- | 
| `lowPoint` | The lower bound of the halved interval. Should be set to the lowest reasonably expected value of maximum performance. | `0` users per second |
| `highPoint` | The upper bound of the halved interval. | `10` users per second |
| `tolerance` | Indicates the precision of measurement. The stress test loop stops when the size of the halved interval is lower than this value. | `1` users per second |

