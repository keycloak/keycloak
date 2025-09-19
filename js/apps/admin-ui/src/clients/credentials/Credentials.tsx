import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import {
  HelpItem,
  SelectControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  Card,
  CardBody,
  ClipboardCopy,
  Divider,
  Form,
  FormGroup,
  PageSection,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { FormFields } from "../ClientDetails";
import { ClientSecret } from "./ClientSecret";
import { SignedJWT } from "./SignedJWT";
import { X509 } from "./X509";
import { convertAttributeNameToForm } from "../../util";

type AccessToken = {
  registrationAccessToken: string;
};

export type CredentialsProps = {
  client: ClientRepresentation;
  save: () => void;
  refresh: () => void;
};

export const Credentials = ({ client, save, refresh }: CredentialsProps) => {
  const { adminClient } = useAdminClient();

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

  const selectedProvider = providers.find(
    (provider) => provider.id === clientAuthenticatorType,
  );

  const { componentTypes } = useServerInfo();
  const providerProperties = useMemo(
    () =>
      componentTypes?.["org.keycloak.authentication.ClientAuthenticator"]?.find(
        (p) => p.id === clientAuthenticatorType,
      )?.clientProperties,
    [clientAuthenticatorType, componentTypes],
  );

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
        className="pf-v5-u-mt-md"
        role="manage-clients"
        fineGrainedAccess={client.access?.configure}
      >
        <ClientSecretConfirm />
        <AccessTokenConfirm />
        <Card isFlat>
          <CardBody>
            <SelectControl
              name="clientAuthenticatorType"
              label={t("clientAuthenticator")}
              labelIcon={t("clientAuthenticatorTypeHelp")}
              controller={{
                defaultValue: "",
              }}
              options={providers.map(({ id, displayName }) => ({
                key: id!,
                value: displayName || id!,
              }))}
            />
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
            {providerProperties && (
              <Form>
                <DynamicComponents
                  properties={providerProperties}
                  convertToName={(name) =>
                    convertAttributeNameToForm(`attributes.${name}`)
                  }
                />
              </Form>
            )}
            <ActionGroup>
              <Button variant="primary" type="submit" isDisabled={!isDirty}>
                {t("save")}
              </Button>
            </ActionGroup>
          </CardBody>
          {selectedProvider?.supportsSecret && (
            <>
              <Divider />
              <CardBody>
                <ClientSecret
                  client={client}
                  secret={secret}
                  toggle={toggleClientSecretConfirm}
                />
              </CardBody>
            </>
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
