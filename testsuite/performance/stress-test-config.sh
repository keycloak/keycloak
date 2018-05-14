#!/bin/bash

# common settings
export algorithm=incremental
export provisioning=false
export maxIterations=10

export dataset=100u2c
export warmUpPeriod=120
export sequentialUsersFrom=-1

# incremental 
export usersPerSec0=5
export incrementFactor=1

# bisection
export lowPoint=0.000
export highPoint=10.000
export tolerance=1.000

# other
export debug=false
