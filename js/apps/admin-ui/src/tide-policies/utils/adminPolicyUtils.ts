/** TIDECLOAK IMPLEMENTATION */

import { joinPath } from "../../utils/joinPath";

export interface ParsedAdminPolicy {
  version: string;
  contractId: string;
  modelId: string;
  keyId: string;
  approvalType: string;
  executionType: string;
  params: Record<string, string>;
  dataToVerifyLength: string;
  signature: string;
}

/**
 * Fetches the admin policy from the non-admin RealmResourceProvider endpoint:
 * GET /realms/{realm}/tide-policy-resources/admin-policy
 */
export async function fetchAdminPolicy(
  baseUrl: string,
  realmName: string,
  accessToken: string | undefined,
): Promise<string> {
  const url = joinPath(
    baseUrl,
    "realms",
    encodeURIComponent(realmName),
    "tide-policy-resources",
    "admin-policy",
    "display",
  );

  const headers: Record<string, string> = {};
  if (accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`;
  }

  const response = await fetch(url, { method: "GET", headers });

  if (!response.ok) {
    throw new Error(`Failed to fetch admin policy: ${response.status}`);
  }

  return response.text();
}

/**
 * Parses a Policy(...) string returned by Policy.toString() into structured data.
 *
 * Example input:
 *   Policy(Version=2, ContractId=GenericResourceAccessThresholdRole:1, ModelId=any,
 *   KeyId=6267..., ApprovalType=EXPLICIT, ExecutionType=PUBLIC,
 *   Params={role="tide-realm-admin", resource="realm-management", threshold=1},
 *   DataToVerifyLength=291, Signature=90E9...)
 */
export function parseAdminPolicy(raw: string): ParsedAdminPolicy {
  const s = raw.replace(/^Policy\(/, "").replace(/\)$/, "").trim();
  const fields: Record<string, string> = {};
  let pos = 0;

  while (pos < s.length) {
    // Skip separators
    while (pos < s.length && (s[pos] === "," || s[pos] === " ")) pos++;
    if (pos >= s.length) break;

    // Read key
    const eq = s.indexOf("=", pos);
    if (eq === -1) break;
    const key = s.substring(pos, eq).trim();
    pos = eq + 1;

    // Read value
    if (pos < s.length && s[pos] === "{") {
      // Brace-delimited value (e.g. Params={...})
      let depth = 0;
      let end = pos;
      do {
        if (s[end] === "{") depth++;
        if (s[end] === "}") depth--;
        end++;
      } while (end < s.length && depth > 0);
      fields[key] = s.substring(pos + 1, end - 1);
      pos = end;
    } else {
      // Simple value — ends at next ", "
      const nextComma = s.indexOf(", ", pos);
      if (nextComma === -1) {
        fields[key] = s.substring(pos).trim();
        break;
      } else {
        fields[key] = s.substring(pos, nextComma).trim();
        pos = nextComma;
      }
    }
  }

  // Parse Params sub-fields: role="value", resource="value", threshold=1
  const params: Record<string, string> = {};
  if (fields["Params"]) {
    for (const part of fields["Params"].split(", ")) {
      const eqIdx = part.indexOf("=");
      if (eqIdx === -1) continue;
      const k = part.substring(0, eqIdx).trim();
      let v = part.substring(eqIdx + 1).trim();
      if (v.startsWith('"') && v.endsWith('"')) {
        v = v.substring(1, v.length - 1);
      }
      params[k] = v;
    }
  }

  return {
    version: fields["Version"] || "",
    contractId: fields["ContractId"] || "",
    modelId: fields["ModelId"] || "",
    keyId: fields["KeyId"] || "",
    approvalType: fields["ApprovalType"] || "",
    executionType: fields["ExecutionType"] || "",
    params,
    dataToVerifyLength: fields["DataToVerifyLength"] || "",
    signature: fields["Signature"] || "",
  };
}
