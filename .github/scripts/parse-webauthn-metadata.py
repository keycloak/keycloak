#!/usr/bin/env python
#
# Parses the passkey authenticator AAGUID registry (combined_aaguid.json) and produces:
#   1. keycloak-webauthn-metadata.json  — metadata consumed by the Keycloak server at runtime
#   2. Passkey icon image files         — decoded from base64 data-URIs in the source
#
# The icon files are written to two directories so both the login theme and the
# account console can reference them:
#   - js/apps/account-ui/public/passkeys/
#   - themes/src/main/resources/theme/base/login/resources/img/passkeys/
#
# See services/src/main/resources/README.md for full usage instructions.

import base64
import json
import os
import re
import sys
import unicodedata

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

ICON_DEST_DIRS = [
    os.path.join(REPO_ROOT, "js", "apps", "account-ui", "public", "passkeys"),
    os.path.join(REPO_ROOT, "themes", "src", "main", "resources", "theme", "base", "login", "resources", "img", "passkeys"),
]

METADATA_DEST = os.path.join(REPO_ROOT, "services", "src", "main", "resources", "keycloak-webauthn-metadata.json")


def normalize(input_str):
    """Strip diacritics so authenticator names become safe ASCII filenames."""
    nfkd_form = unicodedata.normalize('NFKD', input_str)
    return u"".join([c for c in nfkd_form if not unicodedata.combining(c)])



def write_file(name, data):
    """Write binary data to every icon destination directory."""
    if os.sep in name or name in ('.', '..') or '/' in name or not name:
        print(f"Rejecting unsafe filename: '{name}'", file=sys.stderr)
        return
    for dest_dir in ICON_DEST_DIRS:
        os.makedirs(dest_dir, exist_ok=True)
        with open(os.path.join(dest_dir, name), "wb") as f:
            f.write(data)


def decode_base64(base64_str):
    """Decode a base64 string with padding correction."""
    base64_str += "=" * ((4 - len(base64_str) % 4) % 4)
    return base64.b64decode(base64_str)


def process_icon(icon_data_uri, short_name, flavor):
    """Extract an image from a data-URI, write it to disk, and return the filename."""
    if icon_data_uri is None:
        return None

    if icon_data_uri.startswith('data:image/svg+xml;base64,'):
        base64_str = icon_data_uri[len('data:image/svg+xml;base64,'):]
        name = short_name + "-" + flavor + ".svg"
        write_file(name, decode_base64(base64_str))
        return name
    elif icon_data_uri.startswith('data:image/png;base64,'):
        base64_str = icon_data_uri[len('data:image/png;base64,'):]
        name = short_name + "-" + flavor + ".png"
        write_file(name, decode_base64(base64_str))
        return name
    else:
        print("Unknown data image format: " + icon_data_uri[:40], file=sys.stderr)
        return None


def parse_aaguids(input_file):
    """
    Read the given combined_aaguid.json file and produce
    keycloak-webauthn-metadata.json plus decoded icon files.
    """
    names = set()
    files = {}
    output = {}

    with open(input_file, 'r', encoding='utf-8') as f:
        contents = json.load(f)

    for aaguid, info in contents.items():
        name = info.get('name')
        if name is None:
            continue

        # Derive a short filesystem-safe name from the authenticator name
        short_name = normalize(name)
        short_name = re.split(r'[^0-9a-zA-Z_\-]', short_name)[0].lower()
        if not short_name:
            print(f"Skipping entry with no usable name: '{name}' (AAGUID: {aaguid})", file=sys.stderr)
            continue

        prefix = short_name
        i = 0
        while short_name in names:
            i += 1
            short_name = prefix + str(i)
        names.add(short_name)

        icon_light = info.get('icon_light')
        icon_dark = info.get('icon_dark')

        # Deduplicate identical icons across authenticators
        if icon_light is not None and icon_light in files:
            file_light = files[icon_light]
        else:
            file_light = process_icon(icon_light, short_name, 'light')
            if icon_light is not None:
                files[icon_light] = file_light

        if icon_dark is not None and icon_dark in files:
            file_dark = files[icon_dark]
        else:
            file_dark = process_icon(icon_dark, short_name, 'dark')
            if icon_dark is not None:
                files[icon_dark] = file_dark

        entry = {"name": name}
        if file_light is not None:
            entry["icon_light"] = file_light
        if file_dark is not None:
            entry["icon_dark"] = file_dark

        output[aaguid] = entry

    with open(METADATA_DEST, 'w') as f:
        json.dump(output, f, indent=2)
        f.write('\n')

    print(f"Wrote {len(output)} entries to {METADATA_DEST}")
    print(f"Icons written to: {', '.join(ICON_DEST_DIRS)}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <combined_aaguid.json>", file=sys.stderr)
        sys.exit(1)
    parse_aaguids(sys.argv[1])
