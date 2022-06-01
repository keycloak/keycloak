#! /bin/bash
set -euxo pipefail

mkdir -p /tmp/olm/
curl -L https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.20.0/install.sh -o /tmp/olm/install.sh
chmod +x /tmp/olm/install.sh
/tmp/olm/install.sh v0.20.0
