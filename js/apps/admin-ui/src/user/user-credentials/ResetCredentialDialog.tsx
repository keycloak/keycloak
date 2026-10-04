import type { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { AlertVariant, Form, ModalVariant } from "@patternfly/react-core";
import { isEmpty } from "lodash-es";
import { useEffect, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts, TextControl } from "@keycloak/keycloak-ui-shared";
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

const isValidRedirectUri = (value: string) => {
  if (!value) {
    return true;
  }
  // Mirror the server's URI.create strictness (RedirectUtils.toUri):
  // raw whitespace and malformed percent-escapes never survive it,
  // so block them here instead of waiting for the server 400.
  if (/\s/.test(value)) {
    return false;
  }
  try {
    decodeURIComponent(value);
  } catch {
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
  if (!URI_SCHEME_PATTERN.test(value)) {
    return false;
  }
  try {
    new URL(value);
    return true;
  } catch {
    return false;
  }
};

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

  const { handleSubmit, control, trigger } = form;

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

  useEffect(() => {
    void trigger("redirectUri");
  }, [clientIdWatcher, trigger]);

  useEffect(() => {
    void trigger("clientId");
  }, [redirectUriWatcher, trigger]);

  const [allowedRedirects, setAllowedRedirects] = useState<string[]>([]);

  useEffect(() => {
    const clientId = clientIdWatcher?.trim();
    setAllowedRedirects([]);
    if (!clientId) {
      return;
    }
    let cancelled = false;
    adminClient.clients
      .find({ clientId })
      .then(
        (clients) =>
          !cancelled &&
          setAllowedRedirects(
            clients.find((client) => client.clientId === clientId)
              ?.redirectUris ?? [],
          ),
      )
      .catch(() => !cancelled && setAllowedRedirects([]));
    return () => {
      cancelled = true;
    };
  }, [adminClient, clientIdWatcher]);

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

    if (submitRedirectUri && !isValidRedirectUri(submitRedirectUri)) {
      return;
    }

    if (submitRedirectUri && !submitClientId) {
      return;
    }

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
            isRequired={!!redirectUriWatcher?.trim()}
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
                    uris: allowedRedirects.join(", "),
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
                return true;
              },
            }}
          />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
