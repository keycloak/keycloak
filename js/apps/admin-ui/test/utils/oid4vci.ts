import { test } from "@playwright/test";
import adminClient from "./AdminClient.ts";

export const OID4VCI_SERVER_FEATURE = "OID4VC_VCI";
export const OID4VCI_MDOC_SERVER_FEATURE = "OID4VC_MDOC";
export const OID4VCI_PROTOCOL = "OpenID for Verifiable Credentials";
export const OID4VCI_UNAVAILABLE_MESSAGE =
  "OID4VCI protocol is unavailable. Start Keycloak with verifiable credentials support enabled.";
export const OID4VCI_MDOC_UNAVAILABLE_MESSAGE =
  "OID4VCI mDoc support is unavailable. Start Keycloak with oid4vc-mdoc support enabled.";

const REQUIRE_OID4VCI = process.env.KEYCLOAK_REQUIRE_OID4VCI === "true";

export async function skipIfOID4VCIFeatureDisabled() {
  const isOID4VCIFeatureEnabled = await adminClient.isFeatureEnabled(
    OID4VCI_SERVER_FEATURE,
  );

  if (!isOID4VCIFeatureEnabled && REQUIRE_OID4VCI) {
    throw new Error(
      `${OID4VCI_UNAVAILABLE_MESSAGE} KEYCLOAK_REQUIRE_OID4VCI=true requires this feature.`,
    );
  }

  // eslint-disable-next-line playwright/no-skipped-test -- This gate documents when the server cannot run OID4VCI tests.
  test.skip(!isOID4VCIFeatureEnabled, OID4VCI_UNAVAILABLE_MESSAGE);
}

export async function skipIfOID4VCIMdocFeatureDisabled() {
  const isOID4VCIMdocFeatureEnabled = await adminClient.isFeatureEnabled(
    OID4VCI_MDOC_SERVER_FEATURE,
  );

  // eslint-disable-next-line playwright/no-skipped-test -- This gate documents when the server cannot run mDoc tests.
  test.skip(!isOID4VCIMdocFeatureEnabled, OID4VCI_MDOC_UNAVAILABLE_MESSAGE);
}
