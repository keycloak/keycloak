import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  Card,
  CardBody,
  ClipboardCopy,
  Divider,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { useFetch } from "../../utils/useFetch";
import { FormFields } from "../ClientDetails";
import { ClientSecret } from "./ClientSecret";
import { SignedJWT } from "./SignedJWT";
import { X509 } from "./X509";

type AccessToken = {
  registrationAccessToken: string;
};

export type CredentialsProps = {
  client: ClientRepresentation;
  save: () => void;
  refresh: () => void;
};

export const Credentials = ({ client, save, refresh }: CredentialsProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const clientId = client.id!;

  const [providers, setProviders] = useState<
    AuthenticationProviderRepresentation[]
  >([]);

  const {
    control,
    formState: { isDirty },
    handleSubmit,
  } = useFormContext<FormFields>();

  const clientAuthenticatorType = useWatch({
    control: control,
    name: "clientAuthenticatorType",
    defaultValue: "",
  });

  const [secret, setSecret] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [open, isOpen] = useState(false);

  useFetch(
    () =>
      Promise.all([
        adminClient.authenticationManagement.getClientAuthenticatorProviders(),
        adminClient.clients.getClientSecret({
          id: clientId,
        }),
      ]),
    ([providers, secret]) => {
      setProviders(providers);
      setSecret(secret.value!);
    },
    [],
  );

  async function regenerate<T>(
    call: (clientId: string) => Promise<T>,
    message: string,
  ): Promise<T | undefined> {
    try {
      const data = await call(clientId);
      addAlert(t(`${message}Success`), AlertVariant.success);
      return data;
    } catch (error) {
      addError(`${message}Error`, error);
    }
  }

  const regenerateClientSecret = async () => {
    const secret = await regenerate<CredentialRepresentation>(
      (clientId) =>
        adminClient.clients.generateNewClientSecret({ id: clientId }),
      "clientSecret",
    );
    setSecret(secret?.value || "");
    refresh();
  };

  const [toggleClientSecretConfirm, ClientSecretConfirm] = useConfirmDialog({
    titleKey: "confirmClientSecretTitle",
    messageKey: "confirmClientSecretBody",
    continueButtonLabel: "yes",
    cancelButtonLabel: "no",
    onConfirm: regenerateClientSecret,
  });

  const regenerateAccessToken = async () => {
    const accessToken = await regenerate<AccessToken>(
      (clientId) =>
        adminClient.clients.generateRegistrationAccessToken({ id: clientId }),
      "accessToken",
    );
    setAccessToken(accessToken?.registrationAccessToken || "");
  };

  const [toggleAccessTokenConfirm, AccessTokenConfirm] = useConfirmDialog({
    titleKey: "confirmAccessTokenTitle",
    messageKey: "confirmAccessTokenBody",
    continueButtonLabel: "yes",
    cancelButtonLabel: "no",
    onConfirm: regenerateAccessToken,
  });

  return (
    <PageSection>
      <FormAccess
        onSubmit={handleSubmit(save)}
        isHorizontal
        className="pf-u-mt-md"
        role="manage-clients"
        fineGrainedAccess={client.access?.configure}
      >
        <ClientSecretConfirm />
        <AccessTokenConfirm />
        <Card isFlat>
          <CardBody>
            <FormGroup
              label={t("clientAuthenticator")}
              fieldId="kc-client-authenticator-type"
              labelIcon={
                <HelpItem
                  helpText={t("clientAuthenticatorTypeHelp")}
                  fieldLabelId="clientAuthenticator"
                />
              }
            >
              <Controller
                name="clientAuthenticatorType"
                control={control}
                defaultValue=""
                render={({ field }) => (
                  <Select
                    toggleId="kc-client-authenticator-type"
                    required
                    onToggle={isOpen}
                    onSelect={(_, value) => {
                      field.onChange(value as string);
                      isOpen(false);
                    }}
                    selections={field.value}
                    variant={SelectVariant.single}
                    aria-label={t("clientAuthenticator")}
                    isOpen={open}
                  >
                    {providers.map((option) => (
                      <SelectOption
                        selected={option.id === field.value}
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
            {(clientAuthenticatorType === "client-jwt" ||
              clientAuthenticatorType === "client-secret-jwt") && (
              <SignedJWT clientAuthenticatorType={clientAuthenticatorType} />
            )}
            {clientAuthenticatorType === "client-jwt" && (
              <FormGroup>
                <Alert variant="info" isInline title={t("signedJWTConfirm")} />
              </FormGroup>
            )}
            {clientAuthenticatorType === "client-x509" && <X509 />}
            <ActionGroup>
              <Button variant="primary" type="submit" isDisabled={!isDirty}>
                {t("save")}
              </Button>
            </ActionGroup>
          </CardBody>
          {(clientAuthenticatorType === "client-secret" ||
            clientAuthenticatorType === "client-secret-jwt") && <Divider />}
          {(clientAuthenticatorType === "client-secret" ||
            clientAuthenticatorType === "client-secret-jwt") && (
            <CardBody>
              <ClientSecret
                client={client}
                secret={secret}
                toggle={toggleClientSecretConfirm}
              />
            </CardBody>
          )}
        </Card>
        <Card isFlat>
          <CardBody>
            <FormGroup
              label={t("registrationAccessToken")}
              fieldId="kc-access-token"
              labelIcon={
                <HelpItem
                  helpText={t("registrationAccessTokenHelp")}
                  fieldLabelId="registrationAccessToken"
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
                  >
                    {t("regenerate")}
                  </Button>
                </SplitItem>
              </Split>
            </FormGroup>
          </CardBody>
        </Card>
      </FormAccess>
    </PageSection>
  );
};
