import { test } from "@playwright/test";
import { adminClient } from "./admin-client.ts";

const OID4VCI_SERVER_FEATURE = "OID4VC_VCI";
const OID4VCI_UNAVAILABLE_MESSAGE =
  "OID4VCI protocol is unavailable. Start Keycloak with verifiable credentials support enabled.";
const REQUIRE_OID4VCI = process.env.KEYCLOAK_REQUIRE_OID4VCI === "true";

export async function skipIfOID4VCIFeatureDisabled() {
  const features = (await adminClient.serverInfo.find()).features;
  const normalizeServerFeatureName = (name?: string) =>
    name?.replace(/_V\d+$/, "");
  const isEnabled =
    features?.some(
      (feature) =>
        feature.enabled &&
        (feature.name === OID4VCI_SERVER_FEATURE ||
          normalizeServerFeatureName(feature.name) === OID4VCI_SERVER_FEATURE),
    ) ?? false;

  if (!isEnabled && REQUIRE_OID4VCI) {
    throw new Error(
      `${OID4VCI_UNAVAILABLE_MESSAGE} KEYCLOAK_REQUIRE_OID4VCI=true requires this feature.`,
    );
  }

  // eslint-disable-next-line playwright/no-skipped-test -- This gate documents when the server cannot run OID4VCI tests.
  test.skip(!isEnabled, OID4VCI_UNAVAILABLE_MESSAGE);
}
