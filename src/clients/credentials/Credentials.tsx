import React, { useEffect, useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useErrorHandler } from "react-error-boundary";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Card,
  CardBody,
  ClipboardCopy,
  Divider,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import CredentialRepresentation from "keycloak-admin/lib/defs/credentialRepresentation";

import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import {
  useAdminClient,
  asyncStateFetch,
} from "../../context/auth/AdminClient";

import { ClientSecret } from "./ClientSecret";
import { SignedJWT } from "./SignedJWT";
import { X509 } from "./X509";

type ClientAuthenticatorProviders = {
  id: string;
  displayName: string;
};

type Secret = {
  type: string;
  value: string;
};

type AccessToken = {
  registrationAccessToken: string;
};

export type CredentialsProps = {
  clientId: string;
  save: () => void;
};

export const Credentials = ({ clientId, save }: CredentialsProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const handleError = useErrorHandler();
  const { addAlert } = useAlerts();
  const {
    control,
    formState: { isDirty },
  } = useFormContext();
  const clientAuthenticatorType = useWatch({
    control: control,
    name: "clientAuthenticatorType",
  });

  const [providers, setProviders] = useState<ClientAuthenticatorProviders[]>(
    []
  );
  const [secret, setSecret] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [open, isOpen] = useState(false);

  useEffect(() => {
    return asyncStateFetch(
      async () => {
        const providers = await adminClient.authenticationManagement.getClientAuthenticatorProviders(
          { id: clientId }
        );

        const secret = await adminClient.clients.getClientSecret({
          id: clientId,
        });
        return {
          providers,
          secret: secret.value!,
        };
      },
      ({ providers, secret }) => {
        setProviders(providers);
        setSecret(secret);
      },
      handleError
    );
  }, []);

  async function regenerate<T>(
    call: (clientId: string) => Promise<T>,
    message: string
  ): Promise<T | undefined> {
    try {
      const data = await call(clientId);
      addAlert(t(`${message}Success`), AlertVariant.success);
      return data;
    } catch (error) {
      addAlert(t(`${message}Error`, { error }), AlertVariant.danger);
    }
  }

  const regenerateClientSecret = async () => {
    const secret = await regenerate<CredentialRepresentation>(
      (clientId) =>
        adminClient.clients.generateNewClientSecret({ id: clientId }),
      "clientSecret"
    );
    setSecret(secret?.value || "");
  };

  const [toggleClientSecretConfirm, ClientSecretConfirm] = useConfirmDialog({
    titleKey: "clients:confirmClientSecretTitle",
    messageKey: "clients:confirmClientSecretBody",
    continueButtonLabel: "common:yes",
    cancelButtonLabel: "common:no",
    onConfirm: regenerateClientSecret,
  });

  const regenerateAccessToken = async () => {
    const accessToken = await regenerate<AccessToken>(
      (clientId) =>
        adminClient.clients.generateRegistrationAccessToken({ id: clientId }),
      "accessToken"
    );
    setAccessToken(accessToken?.registrationAccessToken || "");
  };

  const [toggleAccessTokenConfirm, AccessTokenConfirm] = useConfirmDialog({
    titleKey: "clients:confirmAccessTokenTitle",
    messageKey: "clients:confirmAccessTokenBody",
    continueButtonLabel: "common:yes",
    cancelButtonLabel: "common:no",
    onConfirm: regenerateAccessToken,
  });

  return (
    <FormAccess isHorizontal className="pf-u-mt-md" role="manage-clients">
      <ClientSecretConfirm />
      <AccessTokenConfirm />
      <Card isFlat>
        <CardBody>
          <FormGroup
            label={t("clientAuthenticator")}
            fieldId="kc-client-authenticator-type"
            labelIcon={
              <HelpItem
                helpText="clients-help:client-authenticator-type"
                forLabel={t("clientAuthenticator")}
                forID="kc-client-authenticator-type"
              />
            }
          >
            <Controller
              name="clientAuthenticatorType"
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-client-authenticator-type"
                  required
                  onToggle={() => isOpen(!open)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    isOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("clientAuthenticator")}
                  isOpen={open}
                >
                  {providers.map((option) => (
                    <SelectOption
                      selected={option.id === value}
                      key={option.id}
                      value={option.id}
                    >
                      {option.displayName}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          {clientAuthenticatorType === "client-jwt" && <SignedJWT />}
          {clientAuthenticatorType === "client-x509" && <X509 />}
          <ActionGroup>
            <Button
              variant="primary"
              onClick={() => save()}
              isDisabled={!isDirty}
            >
              {t("common:save")}
            </Button>
          </ActionGroup>
        </CardBody>
        <CardBody>
          {(clientAuthenticatorType === "client-secret" ||
            clientAuthenticatorType === "client-secret-jwt") && (
            <>
              <Divider className="pf-u-mb-md" />
              <ClientSecret
                secret={secret}
                toggle={toggleClientSecretConfirm}
              />
            </>
          )}
        </CardBody>
      </Card>
      <Card isFlat>
        <CardBody>
          <FormGroup
            label={t("registrationAccessToken")}
            fieldId="kc-access-token"
            labelIcon={
              <HelpItem
                helpText="clients-help:registration-access-token"
                forLabel={t("registrationAccessToken")}
                forID="kc-access-token"
              />
            }
          >
            <Split hasGutter>
              <SplitItem isFilled>
                <ClipboardCopy id="kc-access-token" isReadOnly>
                  {accessToken}
                </ClipboardCopy>
              </SplitItem>
              <SplitItem>
                <Button
                  variant="secondary"
                  onClick={toggleAccessTokenConfirm}
                  isDisabled={isDirty}
                >
                  {t("regenerate")}
                </Button>
              </SplitItem>
            </Split>
          </FormGroup>
        </CardBody>
      </Card>
    </FormAccess>
  );
};
