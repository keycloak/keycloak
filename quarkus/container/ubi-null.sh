#!/bin/bash

set -euo pipefail
#set -x

dir="/tmp/null"
rm -rf "$dir"
mkdir "$dir"
cd "$dir"

# Add all arguments as the initial core packages
printf '%s\n' "$@" > keep
# Packages required for a shell environment
cat >>keep <<EOF
bash
coreutils-single
EOF

# Disallow list to block certain packages and their dependencies
cat >disallow <<EOF
alsa-lib
copy-jdk-configs
cups-libs
chkconfig
info
gawk
platform-python
platform-python-setuptools
python3
python3-libs
python3-pip-wheel
python3-setuptools-wheel
p11-kit

EOF

sort -u keep -o keep

echo "==> Installing packages into chroot" >&2

set -x
# Install requirements for this script (xargs and cmp)
dnf install -y findutils diffutils
# Install core packages to chroot
rootfs="$(realpath rootfs)"
mkdir -p "$rootfs"
<keep xargs dnf install -y --installroot "$rootfs" --releasever 9 --setopt install_weak_deps=false --nodocs
dnf --installroot "$rootfs" clean all
rm -rf "$rootfs"/var/cache/* "$rootfs"/var/log/dnf* "$rootfs"/var/log/yum.*
{ set +x; } 2>/dev/null

echo "==> Building dependency tree" >&2
# Loop until we have the full dependency tree (no new packages found)
touch old
while ! cmp -s keep old
do
    # 1. Get requirement names (not quite the same as package names)
    # 2. Filter out any install-time requirements
    # 3. Query which packages are being used to satisfy the requirements
    # 4. Keep just their package names
    # 5. Remove packages that are on the disallow list
    # 6. Store result as an allowlist
    <keep xargs rpm -r "$rootfs" -q --requires | sort -Vu | cut -d ' ' -f1 \
        | grep -v -e '^rpmlib(' \
        | xargs -d $'\n' rpm -r "$rootfs" -q --whatprovides \
        | grep -v -e '^no package provides' \
        | sed -r 's/^(.*)-.*-.*$/\1/' \
        | grep -vxF -f disallow  \
        > new || true

    # Safely replace the keep list, appending the new names
    mv keep old
    cat old new > keep
    # Sort and deduplicate so cmp will eventually return true
    sort -u keep -o keep
done

# Determine all packages that need to be removed
rpm -r "$rootfs" -qa | sed -r 's/^(.*)-.*-.*$/\1/' | sort -u > all
# Set complement (all - keep)
grep -vxF -f keep all > remove

echo "==> $(wc -l remove | cut -d ' ' -f1) packages to erase:" >&2
cat remove
echo "==> $(wc -l keep | cut -d ' ' -f1) packages to keep:" >&2
cat keep
echo "" >&2

echo "==> Erasing packages" >&2
# Delete all packages that aren't needed for the core packages
set -x
<remove xargs rpm -r "$rootfs" --erase --nodeps --allmatches
{ set +x; } 2>/dev/null

echo "" >&2
echo "==> Packages erased ok!" >&2
