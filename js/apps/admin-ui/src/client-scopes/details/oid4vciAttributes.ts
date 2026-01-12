import { ClientScopeDefaultOptionalType } from "../../components/client-scope/ClientScopeTypes";
import { convertAttributeNameToForm } from "../../util";

/* OID4VC attributes we explicitly clean up when empty/whitespace-only.
   Keep this list in sync with optional OID4VC form fields; add to it when
   new string/number-like attributes are introduced that should be pruned. */
export const OID4VC_ATTRIBUTE_KEYS = [
  "vc.credential_configuration_id",
  "vc.credential_identifier",
  "vc.issuer_did",
  "vc.expiry_in_seconds",
  "vc.credential_build_config.token_jws_type",
  "vc.supported_credential_types",
  "vc.credential_signing_alg",
  "vc.verifiable_credential_type",
  "vc.credential_build_config.sd_jwt.visible_claims",
  "vc.display",
] as const;

const isEmptyValue = (value: unknown) =>
  value === null ||
  value === undefined ||
  (typeof value === "string" && value.trim() === "");

/** Prune known optional OID4VC attributes from the payload when they are empty. */
export const removeEmptyOid4vcAttributes = (
  values: ClientScopeDefaultOptionalType,
): ClientScopeDefaultOptionalType => {
  const fieldNames = OID4VC_ATTRIBUTE_KEYS.map((attr) =>
    convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
      `attributes.${attr}`,
    ),
  );

  /* Shallow copies are sufficient while OID4VC attributes stay flat; if we add
     nested objects under attributes.vc.* we should switch to a deep clone here. */
  const cleanedValues = { ...values } as Record<string, unknown>;
  const hadAttributes = Boolean(cleanedValues.attributes);
  const cleanedAttributes = {
    ...(cleanedValues.attributes as Record<string, unknown> | undefined),
  };

  for (const fieldName of fieldNames) {
    const attrKey = fieldName.replace(/^attributes\./, "");
    if (isEmptyValue(cleanedAttributes?.[attrKey])) {
      delete cleanedAttributes[attrKey];
    }
  }

  if (Object.keys(cleanedAttributes).length === 0) {
    if (hadAttributes) {
      delete cleanedValues.attributes;
    }
  } else {
    cleanedValues.attributes = cleanedAttributes;
  }

  return cleanedValues as unknown as ClientScopeDefaultOptionalType;
};
