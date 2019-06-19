# Keycloak Performance Testsuite - Stress Testing

## Requirements

- Bash
- `bc`: Arbitrary precision calculator.

## Stress Test

The performance testsuite contains a stress-testing script: `stress-test.sh`.

The stress test is implemented as a loop of individual performance test runs.
The script supports two algorithms:
- incremental (default)
- bisection

The *incremental algorithm* loop starts from a base load and then increases the load by a specified amount in each iteration.
The loop ends when a performance test fails, or when the maximum number of iterations is reached.

The *bisection algorithm* loop has a lower and an upper bound, and a resolution parameter.
In each iteration the middle of the interval is used as a value for the performance test load.
Depending on whether the test passes or fails the lower or upper half of the interval is used for the next iteration.
The loop ends if size of the interval is lower than the specified resolution, or when the maximum number of iterations is reached.

## Usage

```
export PARAMETER1=value1
export PARAMETER2=value2
...
stress-test.sh [-DadditionalTestsuiteParam1=value1 -DadditionalTestsuiteParam2=value2 ...]
```

## Parameters

### Script Execution Parameters

| Variable | Description | Default Value |
| --- | --- | --- | 
| `MVN` | The base Maven command to be used. | `mvn` |
| `KEYCLOAK_PROJECT_HOME` | Root directory of the Keycloak project. | Root directory relative to the location of the `stress-test.sh` script. |
| `DRY_RUN` | Don't execute performance tests. Only print out execution information for each iteration. | `false` |

### Performance Testuite Parameters

| Variable | Description | Default Value |
| --- | --- | --- | 
| `DATASET` | Dataset to be used. | `1r_10c_100u`  |
| `WARMUP_PERIOD` | Value of `warmUpPeriod` testsuite parameter. | `120` seconds  |
| `RAMPUP_PERIOD` | Value of `rampUpPeriod` testsuite parameter. | `60` seconds  |
| `MEASUREMENT_PERIOD` | Value of `measurementPeriod` testsuite parameter. | `120` seconds  |
| `FILTER_RESULTS` | Value of `filterResults` testsuite parameter. Should be enabled. | `true` |
| `@` | Any parameters provided to the `stress-test.sh` script will be passed to the performance testsuite. Optional. | |

### Stress Test Parameters

| Variable | Description | Default Value |
| --- | --- | --- | 
| `STRESS_TEST_ALGORITHM` | Stress test loop algorithm: `incremental` or `bisection`. | `incremental`  |
| `STRESS_TEST_MAX_ITERATIONS` | Maximum number of stress test loop iterations. | `10` iterations |
| `STRESS_TEST_PROVISIONING` | Should the system be re-provisioned in each iteration? If enabled the dataset DB dump is re-imported and the warmup is run in each iteration. | `false` |
| `STRESS_TEST_PROVISIONING_GENERATE_DATASET` | Should the dataset be generated, instead of imported from DB dump? | `false` |
| `STRESS_TEST_PROVISIONING_PARAMETERS` | Additional parameters for the provisioning command. Optional. | |

#### Incremental Algorithm

| Variable | Description | Default Value |
| --- | --- | --- | 
| `STRESS_TEST_UPS_FIRST` | Value of `usersPerSec` parameter in the first iteration. | `1.000` users per second |
| `STRESS_TEST_UPS_INCREMENT` | Increment of `usersPerSec` parameter for each subsequent iteration. | `1.000` users per second |

#### Bisection Algorithm

| Variable | Description | Default Value |
| --- | --- | --- | 
| `STRESS_TEST_UPS_LOWER_BOUND` | Lower bound of `usersPerSec` parameter. | `0.000` users per second |
| `STRESS_TEST_UPS_UPPER_BOUND` | Upper bound of `usersPerSec` parameter. | `10.000` users per second |
| `STRESS_TEST_UPS_RESOLUTION` | Required resolution of the bisection algorithm. | `1.000` users per second |

