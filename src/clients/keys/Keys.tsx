import { useState } from "react";
import { useTranslation } from "react-i18next";
import FileSaver from "file-saver";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  PageSection,
  Switch,
  Text,
  TextContent,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { FormAccess } from "../../components/form-access/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { GenerateKeyDialog } from "./GenerateKeyDialog";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import useToggle from "../../utils/useToggle";
import { convertAttributeNameToForm } from "../../util";
import { ImportKeyDialog, ImportFile } from "./ImportKeyDialog";
import { Certificate } from "./Certificate";

type KeysProps = {
  save: () => void;
  clientId: string;
  hasConfigureAccess?: boolean;
};

const attr = "jwt.credential";

export const Keys = ({ clientId, save, hasConfigureAccess }: KeysProps) => {
  const { t } = useTranslation("clients");
  const {
    control,
    register,
    getValues,
    formState: { isDirty },
  } = useFormContext<ClientRepresentation>();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [keyInfo, setKeyInfo] = useState<CertificateRepresentation>();
  const [openGenerateKeys, toggleOpenGenerateKeys, setOpenGenerateKeys] =
    useToggle();
  const [openImportKeys, toggleOpenImportKeys, setOpenImportKeys] = useToggle();

  const useJwksUrl = useWatch({
    control,
    name: "attributes.use.jwks.url",
    defaultValue: "false",
  });

  useFetch(
    () => adminClient.clients.getKeyInfo({ id: clientId, attr }),
    (info) => setKeyInfo(info),
    []
  );

  const generate = async (config: KeyStoreConfig) => {
    try {
      const keyStore = await adminClient.clients.generateAndDownloadKey(
        {
          id: clientId,
          attr,
        },
        config
      );
      FileSaver.saveAs(
        new Blob([keyStore], { type: "application/octet-stream" }),
        `keystore.${config.format == "PKCS12" ? "p12" : "jks"}`
      );
      addAlert(t("generateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:generateError", error);
    }
  };

  const importKey = async (importFile: ImportFile) => {
    try {
      const formData = new FormData();
      const { file, ...rest } = importFile;
      Object.entries(rest).map((entry) =>
        formData.append(entry[0], entry[1] as string)
      );
      formData.append("file", file.value);

      await adminClient.clients.uploadCertificate(
        { id: clientId, attr },
        formData
      );
      addAlert(t("importSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:importError", error);
    }
  };

  return (
    <PageSection variant="light" className="keycloak__form">
      {openGenerateKeys && (
        <GenerateKeyDialog
          clientId={getValues("clientId")!}
          toggleDialog={toggleOpenGenerateKeys}
          save={generate}
        />
      )}
      {openImportKeys && (
        <ImportKeyDialog toggleDialog={toggleOpenImportKeys} save={importKey} />
      )}
      <Card isFlat>
        <CardHeader>
          <CardTitle>{t("jwksUrlConfig")}</CardTitle>
        </CardHeader>
        <CardBody>
          <TextContent>
            <Text>{t("keysIntro")}</Text>
          </TextContent>
        </CardBody>
        <CardBody>
          <FormAccess
            role="manage-clients"
            fineGrainedAccess={hasConfigureAccess}
            isHorizontal
          >
            <FormGroup
              hasNoPaddingTop
              label={t("useJwksUrl")}
              fieldId="useJwksUrl"
              labelIcon={
                <HelpItem
                  helpText="clients-help:useJwksUrl"
                  fieldLabelId="clients:useJwksUrl"
                />
              }
            >
              <Controller
                name={convertAttributeNameToForm("attributes.use.jwks.url")}
                defaultValue="false"
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    data-testid="useJwksUrl"
                    id="useJwksUrl-switch"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value === "true"}
                    onChange={(value) => onChange(`${value}`)}
                  />
                )}
              />
            </FormGroup>
            {useJwksUrl !== "true" &&
              (keyInfo ? (
                <Certificate plain keyInfo={keyInfo} />
              ) : (
                "No client certificate configured"
              ))}
            {useJwksUrl === "true" && (
              <FormGroup
                label={t("jwksUrl")}
                fieldId="jwksUrl"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:jwksUrl"
                    fieldLabelId="clients:jwksUrl"
                  />
                }
              >
                <KeycloakTextInput
                  type="text"
                  id="jwksUrl"
                  name={convertAttributeNameToForm("attributes.jwks.url")}
                  ref={register}
                />
              </FormGroup>
            )}
            <ActionGroup>
              <Button
                data-testid="saveKeys"
                onClick={save}
                isDisabled={!isDirty}
              >
                {t("common:save")}
              </Button>
              <Button
                data-testid="generate"
                variant="secondary"
                onClick={() => setOpenGenerateKeys(true)}
              >
                {t("generateNewKeys")}
              </Button>
              <Button
                data-testid="import"
                variant="secondary"
                onClick={() => setOpenImportKeys(true)}
                isDisabled={useJwksUrl === "true"}
              >
                {t("import")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </CardBody>
      </Card>
    </PageSection>
  );
};
