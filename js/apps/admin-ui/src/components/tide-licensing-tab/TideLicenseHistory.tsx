import React, { useMemo, useState } from "react";
import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableText,
} from "@patternfly/react-table";
import {
  ClipboardCopy,
  ClipboardCopyVariant,
  Tooltip,
  Label,
  Bullseye,
  EmptyState,
  EmptyStateHeader,
  EmptyStateBody,
  Button,
  Spinner,
  AlertVariant,
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

// TIDECLOAK IMPLEMENTATION
export interface License {
  licenseData: any;   // can be JSON string or object
  status: string;
  date: string;       // epoch seconds string OR ms number string
}

type TideLicenseHistoryProps = {
  licenseList: License[];
};

// --- helpers (browser-time only) ---
const tz = Intl.DateTimeFormat().resolvedOptions().timeZone ?? "Local";

/** Normalize epoch input: supports seconds or milliseconds; returns seconds or null if invalid */
function normalizeEpoch(input: unknown): number | null {
  const n = Number(input);
  if (!Number.isFinite(n) || n <= 0) return null;
  if (n >= 1e11) return Math.floor(n / 1000); // treat 11–13 digits as ms
  return Math.floor(n); // seconds
}

type FormattedLocal =
  | { local: string; iso: string; ago: string; dateObj: Date }
  | null;

function formatLocal(epochSeconds: number | null): FormattedLocal {
  if (epochSeconds == null) return null;
  const d = new Date(epochSeconds * 1000);
  if (isNaN(d.getTime())) return null;

  const local = d.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
  const iso = d.toISOString();

  const diff = Date.now() - d.getTime();
  const abs = Math.abs(diff);
  const mins = Math.round(abs / 60000);
  const hrs = Math.round(abs / 3600000);
  const days = Math.round(abs / 86400000);
  const ago =
    abs < 60000
      ? "just now"
      : abs < 3600000
      ? `${mins}m ${diff < 0 ? "from now" : "ago"}`
      : abs < 86400000
      ? `${hrs}h ${diff < 0 ? "from now" : "ago"}`
      : `${days}d ${diff < 0 ? "from now" : "ago"}`;

  return { local, iso, ago, dateObj: d };
}

// --- robust string utilities ---
function safeStringify(val: unknown, pretty = false): string {
  try {
    if (typeof val === "string") return val;
    return JSON.stringify(val, null, pretty ? 2 : 0);
  } catch {
    try {
      return String(val ?? "");
    } catch {
      return "";
    }
  }
}

/** If string & valid JSON → pretty JSON; if object → JSON.stringify; else → raw string */
function coerceJsonString(val: unknown, pretty = false): string {
  if (typeof val === "string") {
    try {
      const parsed = JSON.parse(val);
      return JSON.stringify(parsed, null, pretty ? 2 : 0);
    } catch {
      return val;
    }
  }
  return safeStringify(val, pretty);
}

function truncateMiddle(input: unknown, max = 160) {
  const s = typeof input === "string" ? input : safeStringify(input);
  if (s.length <= max) return s;
  const head = Math.floor(max / 2) - 3;
  const tail = max - head - 3;
  return `${s.slice(0, head)}...${s.slice(-tail)}`;
}

function parseLicenseData(raw: any): null | { gVRK?: string; [k: string]: any } {
  if (raw == null) return null;
  if (typeof raw === "string") {
    try {
      const obj = JSON.parse(raw);
      return obj && typeof obj === "object" ? obj : null;
    } catch {
      return null;
    }
  }
  if (typeof raw === "object") return raw;
  return null;
}

const isActionableStatus = (status: string) =>
  /^(upcoming\s*renewal|active)$/i.test((status || "").trim());

export const TideLicenseHistory: React.FC<TideLicenseHistoryProps> = ({
  licenseList,
}) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  // per-row loading (keyed by row key)
  const [loading, setLoading] = useState<Record<string, boolean>>({});

  const rows = useMemo(() => {
    return (licenseList ?? []).map((lic) => {
      const epoch = normalizeEpoch(lic.date);
      const fmt = formatLocal(epoch);

      // Always produce a string payload for display/copy
      const copyPayload = coerceJsonString(lic.licenseData, /*pretty=*/true);
      const displaySnippet = truncateMiddle(copyPayload, 160);

      return {
        key: `${lic.status}-${epoch ?? "invalid"}-${displaySnippet.length}`,
        original: lic,
        status: lic.status,
        hasValidDate: !!fmt,
        local: fmt?.local ?? "—",
        iso: fmt?.iso ?? "",
        ago: fmt?.ago ?? "",
        epoch,
        copyPayload,
        displaySnippet,
      };
    });
  }, [licenseList]);

  const setRowLoading = (key: string, v: boolean) =>
    setLoading((prev) => ({ ...prev, [key]: v }));

  // SIGN
  const handleSign = async (row: (typeof rows)[number]) => {
    try {
      setRowLoading(row.key, true);
      const parsed = parseLicenseData(row.original.licenseData);
      const gvrk = parsed?.gVRK?.toString?.().trim();

      const message: string | void = await adminClient.tideAdmin.licenseProvider({
        gvrk, // optional, send if present
      });

      addAlert(
        t(
          "LicensingSigningReviewCreated",
          message || "Request to sign new license created"
        ),
        AlertVariant.success
      );
    } catch (error: any) {
      addError("signingNewLicenseError", error);
    } finally {
      setRowLoading(row.key, false);
    }
  };

  // SWITCH
  const handleSwitch = async (row: (typeof rows)[number]) => {
    try {
      setRowLoading(row.key, true);

      const parsed = parseLicenseData(row.original.licenseData);
      const gvrk = parsed?.gVRK?.toString?.().trim();

      if (!gvrk) {
        addError("switchVrkError", new Error("No gVRK found in license data"));
        return;
      }

      const resp = await adminClient.tideAdmin.switchVrk({ gvrk });

      // Try to surface a meaningful message regardless of return type
      let message = "";
      if (typeof resp === "string") {
        message = resp;
      } else if (resp && typeof (resp as any).text === "function") {
        try {
          message = await (resp as any).text();
        } catch {
          message = "";
        }
      }

      addAlert(
        t("switchVrkSuccessful", message || `Switched active configuration to specified GVRK.`),
        AlertVariant.success
      );
    } catch (error: any) {
      addError("switchVrkError", error);
    } finally {
      setRowLoading(row.key, false);
    }
  };

  // ---- Compact EMPTY STATE (short height, no icon) ----
  if (!licenseList || licenseList.length === 0) {
    return (
      <div
        style={{
          border: "1px solid var(--pf-v5-global--BorderColor--100)",
          borderRadius: 6,
          padding: 8,
          minHeight: 80,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Bullseye>
          <EmptyState>
            <EmptyStateHeader titleText="No license history" headingLevel="h4" />
            <EmptyStateBody style={{ marginTop: 4 }}>
              When licenses are generated, they'll appear here.
            </EmptyStateBody>
          </EmptyState>
        </Bullseye>
      </div>
    );
  }

  return (
    <div
      style={{
        maxHeight: 420,
        overflow: "auto",
        border: "1px solid var(--pf-v5-global--BorderColor--100)",
        borderRadius: 6,
      }}
      aria-label="License history table"
    >
      <Table
        variant="compact"
        borders
        aria-label="Tidecloak license history"
        isStriped
        isStickyHeader
      >
        <Thead>
          <Tr>
            <Th width={40}>License</Th>
            <Th>Status</Th>
            <Th>
              Date{" "}
              <Tooltip
                content={
                  <>
                    Displayed in your browser&apos;s timezone: <strong>{tz}</strong>
                  </>
                }
              >
                <InfoCircleIcon
                  style={{ marginLeft: 6, verticalAlign: "text-bottom" }}
                />
              </Tooltip>
            </Th>
            <Th width={20}>Actions</Th>
          </Tr>
        </Thead>
        <Tbody>
          {rows.map((r) => {
            const actionable = isActionableStatus(r.status);
            const isBusy = !!loading[r.key];
            return (
              <Tr key={r.key}>
                <Td dataLabel="License">
                  <TableText wrapModifier="truncate">
                    <ClipboardCopy
                      isCode
                      isReadOnly
                      hoverTip="Copy full JSON"
                      clickTip="Copied!"
                      variant={ClipboardCopyVariant.inline}
                      onCopy={(e) => {
                        if (navigator?.clipboard?.writeText) {
                          e?.preventDefault?.();
                          navigator.clipboard
                            .writeText(r.copyPayload)
                            .catch(() => {});
                        }
                      }}
                    >
                      {r.displaySnippet}
                    </ClipboardCopy>
                  </TableText>
                </Td>

                <Td dataLabel="Status">
                  <Label
                    color={
                      /active|paid|ok|success/i.test(r.status)
                        ? "green"
                        : /pending|processing|upcoming/i.test(r.status)
                        ? "gold"
                        : /expired|failed|unpaid|cancel/i.test(r.status)
                        ? "red"
                        : "grey"
                    }
                    isCompact
                  >
                    {r.status}
                  </Label>
                </Td>

                <Td dataLabel="Date">
                  {r.hasValidDate ? (
                    <Tooltip
                      content={
                        <>
                          <div>
                            <strong>Local:</strong> {r.local}
                          </div>
                          <div>
                            <strong>ISO (UTC representation):</strong> {r.iso}
                          </div>
                          <div>
                            <strong>Time zone:</strong> {tz}
                          </div>
                        </>
                      }
                    >
                      <span aria-label={`Local date ${r.local}`}>
                        {r.local}{" "}
                        <span
                          style={{ opacity: 0.75, fontSize: "0.85em", marginLeft: 6 }}
                          aria-label={`Occurred ${r.ago}`}
                        >
                          · {r.ago}
                        </span>
                      </span>
                    </Tooltip>
                  ) : (
                    <span style={{ opacity: 0.7 }}>—</span>
                  )}
                </Td>

                <Td dataLabel="Actions">
                  {actionable ? (
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: 8,
                        minWidth: 180,
                      }}
                    >
                      <Button
                        variant="primary"
                        onClick={() => handleSign(r)}
                        aria-label="Sign license"
                        style={{ width: "100%" }}
                        isDisabled={isBusy}
                      >
                        {isBusy ? <Spinner size="sm" /> : "Sign"}
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() => handleSwitch(r)}
                        aria-label="Switch license"
                        style={{ width: "100%" }}
                        isDisabled={isBusy}
                      >
                        Switch
                      </Button>
                    </div>
                  ) : (
                    <span style={{ opacity: 0.65 }}>—</span>
                  )}
                </Td>
              </Tr>
            );
          })}
        </Tbody>
      </Table>
    </div>
  );
};
