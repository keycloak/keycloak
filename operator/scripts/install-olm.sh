#! /bin/bash
set -euxo pipefail

mkdir -p /tmp/olm/
curl -f -L https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.26.0/install.sh -o /tmp/olm/install.sh
chmod +x /tmp/olm/install.sh
/tmp/olm/install.sh v0.26.0
