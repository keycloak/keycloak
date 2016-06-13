# Keycloak Adapter Tests - JBoss Remote

## Performance Tests

### Parameters

* Warmup phase
 - `warmup.load` Load during warmup phase (# of clients).
 - `warmup.duration` Duration of warmup phase in seconds.
* Measuremet iterations
 - `initial.load` Load for the initial measurement iteration (# of clients).
 - `load.increase` How many clients to add after each iteration.
 - `load.increase.rate` How many clients to add per second.
 - `measurement.duration` Duration of measurement iteration (in seconds).
* Limits
 - `max.iterations`
 - `max.threads`
* Other
 - `sleep.between.loops` Sleep period between scenario loops.

### Generated Load

Warmup phase and measurement iterations with load-increase phases in between.

    load

    ^
    │
    │                                                                   /
    │                                                         _________/
    │                                                       /|         |
    │                                                      / |         |
    │                                            _________/  |         |
    │                                          /|         |  |         |
    │                                         / |         |  |         |
    │                               _________/  |         |  |         |
    │                             /│         |  |         |  |         |
    │                            / |         |  |         |  |         |
    │                  _________/  |         |  |         |  |         |
    │                /|         |  |         |  |         |  |         |
    │   ____________/ |         |  |         |  |         |  |         |
    │ /|            | |         |  |         |  |         |  |         |
    │/ |            | |         |  |         |  |         |  |         |
    └──|────────────|─|─────────|──|─────────|──|─────────|──|─────────|───────> time

        <--warmup-->   <--it.1->    <--it.2->    <--it.3->    <--it.4->


### Login-Logout Test Scenario

#### Collected Statistics

 - ACCESS_REQUEST_TIME
 - LOGIN_REQUEST_TIME
 - LOGIN_VERIFY_REQUEST_TIME
 - LOGOUT_REQUEST_TIME
 - LOGOUT_VERIFY_REQUEST_TIME

#### Parameters

* Limits
 - `max.login.time.average` Maximum accepted average value of LOGIN_REQUEST_TIME.
 - `max.logout.time.average` Maximum accepted average value of LOGOUT_REQUEST_TIME.
 - `max.timeout.percentage` Maximum accepted timeout percentage for all statistics.

