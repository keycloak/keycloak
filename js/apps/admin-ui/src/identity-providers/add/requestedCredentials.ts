export const REQUESTED_CREDENTIALS = "config.requestedCredentials";
export const SD_JWT_VC_FORMAT = "dc+sd-jwt";

export type CredentialForm = {
  id: string;
  format: string;
  types: string[];
  trustMaterialIdps: string[];
};

export type CredentialSetForm = {
  required: boolean;
  // Field arrays need objects, so each option wraps its credential ids
  options: { credentials: string[] }[];
};

export type RequestedCredentialsForm = {
  credentials: CredentialForm[];
  credentialSets: CredentialSetForm[];
  principal: { credential: string; claim: string };
};

type RequestedCredentials = {
  credentials?: Partial<CredentialForm>[];
  credentialSets?: { options?: string[][]; required?: boolean }[];
  principal?: { credential?: string; claim?: string };
};

export const emptyCredential = (): CredentialForm => ({
  id: "",
  format: SD_JWT_VC_FORMAT,
  types: [],
  trustMaterialIdps: [],
});

const values = (list: unknown): string[] =>
  Array.isArray(list)
    ? list.map((value) => String(value ?? "").trim()).filter((value) => value)
    : [];

export function toRequestedCredentialsForm(
  json?: string,
): RequestedCredentialsForm {
  let requested: RequestedCredentials = {};
  try {
    requested = (json && JSON.parse(json)) || {};
  } catch {
    // The server rejects invalid requested credentials, so the form starts over
  }
  const credentials = (requested.credentials ?? []).map((credential) => ({
    id: credential.id ?? "",
    format: credential.format ?? SD_JWT_VC_FORMAT,
    types: values(credential.types),
    trustMaterialIdps: values(credential.trustMaterialIdps),
  }));
  return {
    credentials: credentials.length > 0 ? credentials : [emptyCredential()],
    credentialSets: (requested.credentialSets ?? []).map((set) => ({
      required: set.required !== false,
      options: (set.options ?? []).map((option) => ({
        credentials: values(option),
      })),
    })),
    principal: {
      credential: requested.principal?.credential ?? "",
      claim: requested.principal?.claim ?? "",
    },
  };
}

// Builds every member explicitly, as the server rejects unknown ones
export function toRequestedCredentialsJson(
  form: RequestedCredentialsForm,
): string {
  const requested: RequestedCredentials = {
    credentials: form.credentials.map((credential) => ({
      id: credential.id.trim(),
      format: credential.format,
      types: values(credential.types),
      trustMaterialIdps: values(credential.trustMaterialIdps),
    })),
  };
  if (form.credentialSets.length > 0) {
    requested.credentialSets = form.credentialSets.map((set) => ({
      options: set.options.map((option) => values(option.credentials)),
      ...(!set.required && { required: false }),
    }));
  }
  requested.principal = {
    credential: form.principal.credential,
    claim: form.principal.claim.trim(),
  };
  return JSON.stringify(requested);
}
