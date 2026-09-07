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
  "vc.verifiable_credential_type",
  "vc.credential_build_config.sd_jwt.visible_claims",
  "vc.display",
  "vc.binding_required",
  "vc.binding_required_proof_types",
  "vc.cryptographic_binding_methods_supported",
  "vc.key_attestations_required",
  "vc.key_attestations_required.key_storage",
  "vc.key_attestations_required.user_authentication",
  "vc.refresh_interval_in_seconds",
] as const;

const isEmptyValue = (value: unknown) =>
  value === null ||
  value === undefined ||
  (typeof value === "string" && value.trim() === "");

/**
 * Normalize known optional OID4VC attributes when they are empty.
 *
 * On create, empty values are omitted so backend defaults can be applied.
 * On edit, empty values are sent as null so the backend receives an explicit clear signal.
 */
export const removeEmptyOid4vcAttributes = (
  values: ClientScopeDefaultOptionalType,
  isEdit = false,
): ClientScopeDefaultOptionalType => {
  const fieldNames = OID4VC_ATTRIBUTE_KEYS.map((attr) =>
    convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
      `attributes.${attr}`,
    ),
  );

  /* Shallow copies are sufficient while OID4VC attributes stay flat; if we add
     nested objects under attributes.vc.* we should switch to a deep clone here. */
  const cleanedValues: ClientScopeDefaultOptionalType = { ...values };
  const hadAttributes = cleanedValues.attributes !== undefined;
  const cleanedAttributes: NonNullable<
    ClientScopeDefaultOptionalType["attributes"]
  > = {
    ...cleanedValues.attributes,
  };

  for (const fieldName of fieldNames) {
    const attrKey = fieldName.replace(/^attributes\./, "");
    if (isEmptyValue(cleanedAttributes[attrKey])) {
      if (isEdit) {
        cleanedAttributes[attrKey] = null;
      } else {
        delete cleanedAttributes[attrKey];
      }
    }
  }

  if (Object.keys(cleanedAttributes).length === 0) {
    if (hadAttributes) {
      delete cleanedValues.attributes;
    }
  } else {
    cleanedValues.attributes = cleanedAttributes;
  }

  return cleanedValues;
};
