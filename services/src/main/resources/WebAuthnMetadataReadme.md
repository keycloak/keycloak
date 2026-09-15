# Updating WebAuthn / Passkey Metadata

The file `keycloak-webauthn-metadata.json` maps passkey authenticator AAGUIDs to
display names and icon filenames. It is generated from the community-maintained
[passkey-authenticator-aaguids](https://github.com/passkeydeveloper/passkey-authenticator-aaguids) registry.

## Steps to update

1. Download the latest source data:

   ```sh
   curl -LO https://raw.githubusercontent.com/passkeydeveloper/passkey-authenticator-aaguids/refs/heads/main/combined_aaguid.json
   ```

2. Run the parser script, passing the downloaded file as an argument:

   ```sh
   python .github/scripts/parse-webauthn-metadata.py combined_aaguid.json
   ```

3. The script produces:
   - `services/src/main/resources/keycloak-webauthn-metadata.json` (overwritten in place)
   - Icon image files in two directories:
     - `js/apps/account-ui/public/passkeys/` (account console)
     - `themes/src/main/resources/theme/base/login/resources/img/passkeys/` (login theme)

4. Review the changes before committing. In particular, inspect SVG icon files
   for malicious content (e.g. `<script>` tags, event handler attributes) since
   they are served by Keycloak and rendered in the browser.
