import type { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { AlertVariant, Form, ModalVariant } from "@patternfly/react-core";
import { isEmpty } from "lodash-es";
import { useEffect, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts, TextControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { ClientSelect } from "../../components/client/ClientSelect";
import { LifespanField } from "./LifespanField";
import { RequiredActionMultiSelect } from "./RequiredActionMultiSelect";
import { useRealm } from "../../context/realm-context/RealmContext";

type ResetCredentialDialogProps = {
  userId: string;
  onClose: () => void;
};

type CredentialResetForm = {
  actions: RequiredActionAlias[];
  lifespan: number | undefined;
  clientId?: string;
  redirectUri?: string;
};

const URI_SCHEME_PATTERN = /^[a-zA-Z][a-zA-Z0-9+.-]*:/;

// Characters Java's URI.create (used by RedirectUtils.toUri on the server)
// always rejects: ASCII controls, space, DEL, plus the RFC 2396
// excluded/delimiter characters. WHATWG URL used below is lenient with
// these, so reject them explicitly to stay consistent with the server.
// Note: non-ASCII printable characters (e.g. Unicode paths or hosts)
// are accepted by URI.create and must stay allowed here as well.
const URI_ILLEGAL_CHARS_PATTERN = /[<>"`\\{}|^|]/;

// ASCII controls, space, DEL, and any other whitespace: URI.create
// rejects them all, so block them here instead of waiting for the
// server 400. (Written without control escapes: no-control-regex
// forbids e.g. \x00 in regex literals.)
const hasIllegalWhitespaceOrControl = (value: string): boolean => {
  if (/\s/.test(value)) {
    return true;
  }
  for (let index = 0; index < value.length; index++) {
    const code = value.charCodeAt(index);
    if (code < 0x20 || code === 0x7f) {
      return true;
    }
  }
  return false;
};

// Forbidden OIDC parameters that the server rejects in redirect URIs.
// Mirrors RedirectUtils.FORBIDDEN_OIDC_PARAMS: names are compared decoded
// and case-insensitively, query and fragment alike.
const SERVER_FORBIDDEN_OIDC_PARAMS = new Set([
  "code",
  "id_token",
  "access_token",
  "token_type",
  "expires_in",
  "state",
  "iss",
  "error",
  "error_description",
  "session_state",
  "response",
  "kc_action",
  "kc_action_status",
]);

const oidcParamName = (token: string): string => {
  const rawName = token.split("=", 1)[0] ?? "";
  try {
    return decodeURIComponent(rawName).toLowerCase();
  } catch {
    return "";
  }
};

const hasForbiddenOidcParam = (value: string): boolean => {
  const hashIndex = value.indexOf("#");
  const end = hashIndex === -1 ? value.length : hashIndex;
  const queryIndex = value.indexOf("?");
  const query =
    queryIndex !== -1 && queryIndex < end
      ? value.slice(queryIndex + 1, end)
      : "";
  const fragment = hashIndex !== -1 ? value.slice(hashIndex + 1) : "";
  return `${query}&${fragment}`
    .split("&")
    .filter((token) => token.length > 0)
    .some((token) => SERVER_FORBIDDEN_OIDC_PARAMS.has(oidcParamName(token)));
};

const INSTALLED_APP_URN = "urn:ietf:wg:oauth:2.0:oob";

const IPV6_HOST_PATTERN = /^\[[0-9a-fA-F:.]+\]$/;

// Brackets are legal only inside an IPv6 host literal. Everywhere else
// (path, query, fragment, userinfo, relative references) Java's
// URI.create rejects them, so mirror that here instead of blanket
// rejecting (which would also block valid IPv6 hosts).
const hasIllegalBrackets = (value: string): boolean => {
  if (!/[[\]]/.test(value)) {
    return false;
  }
  let hostname = "";
  try {
    hostname = new URL(value).hostname;
  } catch {
    // Not an absolute URI: relative references have no host part,
    // so any bracket is illegal.
    return true;
  }
  if (!IPV6_HOST_PATTERN.test(hostname)) {
    return true;
  }
  // Strip the single legal occurrence (scheme + "://" + host) and
  // reject brackets anywhere else.
  const hostIndex = value.indexOf(hostname);
  return /[[\]]/.test(
    value.slice(0, hostIndex) + value.slice(hostIndex + hostname.length),
  );
};

const isValidRedirectUri = (value: string) => {
  if (!value) {
    return true;
  }
  // Mirror the server's URI.create strictness (RedirectUtils.toUri):
  // illegal characters and malformed percent-escapes never survive it,
  // so block them here instead of waiting for the server 400.
  if (
    hasIllegalWhitespaceOrControl(value) ||
    URI_ILLEGAL_CHARS_PATTERN.test(value)
  ) {
    return false;
  }
  try {
    decodeURIComponent(value);
  } catch {
    return false;
  }
  // Installed-app URN is explicitly allowed by the server
  if (value === INSTALLED_APP_URN) {
    return true;
  }
  if (hasIllegalBrackets(value)) {
    return false;
  }
  // Forbidden OIDC params in query or fragment
  if (hasForbiddenOidcParam(value)) {
    return false;
  }
  if (value.startsWith("/") || !URI_SCHEME_PATTERN.test(value)) {
    // Root-relative or bare relative reference. The server resolves it
    // against the client root URL and matches it against the client's
    // registered redirect URIs, which remain the authority.
    return true;
  }
  // Any absolute URI with a scheme is accepted here (http(s), custom app
  // schemes, URNs, ...). Whether it is registered for the selected
  // client is decided by the server, which answers 400 otherwise.
  try {
    new URL(value);
    return true;
  } catch {
    return false;
  }
};

// Escape characters that could break i18n interpolation or inject markup
// when server-provided URIs are rendered in helper text.
const escapeForHelperText = (value: string): string =>
  value.replace(/[{}]/g, "");

export const ResetCredentialDialog = ({
  userId,
  onClose,
}: ResetCredentialDialogProps) => {
  const { adminClient } = useAdminClient();
  const { realmRepresentation: realm } = useRealm();
  const { t } = useTranslation();
  const form = useForm<CredentialResetForm>({
    mode: "onChange",
    defaultValues: {
      actions: [],
      lifespan: realm.actionTokenGeneratedByAdminLifespan,
      clientId: "",
      redirectUri: "",
    },
  });
  const { addAlert, addError } = useAlerts();

  const { handleSubmit, control, getValues } = form;

  const resetActionWatcher = useWatch({
    control,
    name: "actions",
  });
  const redirectUriWatcher = useWatch({
    control,
    name: "redirectUri",
  });
  const clientIdWatcher = useWatch({
    control,
    name: "clientId",
  });
  const resetIsNotDisabled = !isEmpty(resetActionWatcher);

  const trimmedRedirectUri = redirectUriWatcher?.trim() ?? "";
  const trimmedClientId = clientIdWatcher?.trim() ?? "";
  const redirectUriInvalid =
    !!trimmedRedirectUri && !isValidRedirectUri(trimmedRedirectUri);
  const clientMissing = !!trimmedRedirectUri && !trimmedClientId;
  const confirmButtonDisabled =
    !resetIsNotDisabled || redirectUriInvalid || clientMissing;

  // Re-validate the redirect URI when the client changes: the cross-field
  // rule ("redirect requires client") would otherwise leave a stale error
  // behind. One direction only — trigger() updates errors, never the
  // watched values, so this cannot loop.
  useEffect(() => {
    void form.trigger("redirectUri");
  }, [clientIdWatcher, form]);

  const [allowedRedirects, setAllowedRedirects] = useState<string[]>([]);

  useFetch(
    () => {
      const clientId = clientIdWatcher?.trim();
      if (!clientId) {
        return Promise.resolve<string[]>([]);
      }
      // Swallow fetch errors: admins without view-clients permission can
      // still use this dialog, they just don't get the hint.
      return adminClient.clients
        .find({ clientId })
        .then(
          (clients) =>
            clients.find((client) => client.clientId === clientId)
              ?.redirectUris ?? [],
        )
        .catch(() => []);
    },
    setAllowedRedirects,
    [adminClient, clientIdWatcher],
  );

  const sendCredentialsResetEmail = async ({
    actions,
    lifespan,
    clientId,
    redirectUri,
  }: CredentialResetForm) => {
    if (isEmpty(actions)) {
      return;
    }

    const submitRedirectUri = redirectUri?.trim() || undefined;
    const submitClientId = clientId?.trim() || undefined;

    try {
      await adminClient.users.executeActionsEmail({
        id: userId,
        actions,
        lifespan,
        clientId: submitClientId,
        redirectUri: submitRedirectUri,
      });
      addAlert(t("credentialResetEmailSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      addError("credentialResetEmailError", error);
    }
  };

  return (
    <ConfirmDialogModal
      variant={ModalVariant.medium}
      titleKey="credentialReset"
      open
      onCancel={onClose}
      toggleDialog={onClose}
      continueButtonLabel="credentialResetConfirm"
      onConfirm={async () => {
        await handleSubmit(sendCredentialsResetEmail)();
      }}
      confirmButtonDisabled={confirmButtonDisabled}
    >
      <Form
        id="userCredentialsReset-form"
        isHorizontal
        data-testid="credential-reset-modal"
      >
        <FormProvider {...form}>
          <RequiredActionMultiSelect
            name="actions"
            label="resetAction"
            help="resetActions"
          />
          <LifespanField />
          <ClientSelect
            name="clientId"
            label="credentialResetClient"
            helpText="credentialResetClientHelp"
            defaultValue=""
            variant="typeahead"
          />
          <TextControl
            name="redirectUri"
            label={t("credentialResetRedirectUri")}
            labelIcon={t("credentialResetRedirectUriHelp")}
            placeholder="https://app.example.com/callback"
            type="text"
            helperText={
              allowedRedirects.length > 0
                ? t("credentialResetAllowedRedirects", {
                    uris: allowedRedirects.map(escapeForHelperText).join(", "),
                  })
                : undefined
            }
            rules={{
              validate: (value?: string) => {
                if (!value?.trim()) {
                  return true;
                }
                if (!isValidRedirectUri(value.trim())) {
                  return t("credentialResetInvalidRedirectUri");
                }
                // Cross-field: redirect URI requires client
                const clientId = getValues("clientId");
                if (value.trim() && !clientId?.trim()) {
                  return t("credentialResetClientRequired");
                }
                return true;
              },
            }}
          />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
