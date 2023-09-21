import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
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
import { saveAs } from "file-saver";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";
import { useFetch } from "../../utils/useFetch";
import useToggle from "../../utils/useToggle";
import { FormFields } from "../ClientDetails";
import { Certificate } from "./Certificate";
import { GenerateKeyDialog, getFileExtension } from "./GenerateKeyDialog";
import { ImportFile, ImportKeyDialog } from "./ImportKeyDialog";

type KeysProps = {
  save: () => void;
  clientId: string;
  hasConfigureAccess?: boolean;
};

const attr = "jwt.credential";

export const Keys = ({ clientId, save, hasConfigureAccess }: KeysProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    getValues,
    formState: { isDirty },
  } = useFormContext<FormFields>();
  const { addAlert, addError } = useAlerts();

  const [keyInfo, setKeyInfo] = useState<CertificateRepresentation>();
  const [openGenerateKeys, toggleOpenGenerateKeys, setOpenGenerateKeys] =
    useToggle();
  const [openImportKeys, toggleOpenImportKeys, setOpenImportKeys] = useToggle();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const useJwksUrl = useWatch({
    control,
    name: convertAttributeNameToForm<FormFields>("attributes.use.jwks.url"),
    defaultValue: "false",
  });

  useFetch(
    () => adminClient.clients.getKeyInfo({ id: clientId, attr }),
    (info) => setKeyInfo(info),
    [key],
  );

  const generate = async (config: KeyStoreConfig) => {
    try {
      const keyStore = await adminClient.clients.generateAndDownloadKey(
        {
          id: clientId,
          attr,
        },
        config,
      );
      saveAs(
        new Blob([keyStore], { type: "application/octet-stream" }),
        `keystore.${getFileExtension(config.format ?? "")}`,
      );
      addAlert(t("generateSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("generateError", error);
    }
  };

  const importKey = async (importFile: ImportFile) => {
    try {
      const formData = new FormData();
      const { file, ...rest } = importFile;

      for (const [key, value] of Object.entries(rest)) {
        formData.append(key, value);
      }

      formData.append("file", file.value!);

      await adminClient.clients.uploadCertificate(
        { id: clientId, attr },
        formData,
      );
      addAlert(t("importSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("importError", error);
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
                  helpText={t("useJwksUrlHelp")}
                  fieldLabelId="useJwksUrl"
                />
              }
            >
              <Controller
                name={convertAttributeNameToForm("attributes.use.jwks.url")}
                control={control}
                render={({ field }) => (
                  <Switch
                    data-testid="useJwksUrl"
                    id="useJwksUrl-switch"
                    label={t("on")}
                    labelOff={t("off")}
                    isChecked={field.value === "true"}
                    onChange={(value) => field.onChange(`${value}`)}
                    aria-label={t("useJwksUrl")}
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
                    helpText={t("jwksUrlHelp")}
                    fieldLabelId="jwksUrl"
                  />
                }
              >
                <KeycloakTextInput
                  id="jwksUrl"
                  type="url"
                  {...register(
                    convertAttributeNameToForm("attributes.jwks.url"),
                  )}
                />
              </FormGroup>
            )}
            <ActionGroup>
              <Button
                data-testid="saveKeys"
                onClick={save}
                isDisabled={!isDirty}
              >
                {t("save")}
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
