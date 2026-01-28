import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  PageSection,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { convertAttributeNameToForm } from "../../util";
import useToggle from "../../utils/useToggle";
import { FormFields } from "../ClientDetails";
import { Certificate } from "./Certificate";
import { GenerateKeyDialog, getFileExtension } from "./GenerateKeyDialog";
import { ImportFile, ImportKeyDialog } from "./ImportKeyDialog";

type KeysProps = {
  save: () => void;
  refresh: () => void;
  clientId: string;
  hasConfigureAccess?: boolean;
};

const attr = "jwt.credential";

export const Keys = ({
  clientId,
  save,
  refresh: refreshParent,
  hasConfigureAccess,
}: KeysProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const {
    control,
    getValues,
    formState: { isDirty },
  } = useFormContext<FormFields>();
  const { addAlert, addError } = useAlerts();

  const [keyInfo, setKeyInfo] = useState<CertificateRepresentation>();
  const [openGenerateKeys, toggleOpenGenerateKeys, setOpenGenerateKeys] =
    useToggle();
  const [openImportKeys, toggleOpenImportKeys, setOpenImportKeys] = useToggle();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
    refreshParent();
  };

  const useJwksUrl = useWatch({
    control,
    name: convertAttributeNameToForm<FormFields>("attributes.use.jwks.url"),
    defaultValue: "false",
  });

  useFetch(
    async () => {
      try {
        return await adminClient.clients.getKeyInfo({ id: clientId, attr });
      } catch (error) {
        addError("getKeyInfoError", error);
        return {} as CertificateRepresentation;
      }
    },
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

      formData.append("file", file);
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
            <DefaultSwitchControl
              name={convertAttributeNameToForm("attributes.use.jwks.url")}
              label={t("useJwksUrl")}
              labelIcon={t("useJwksUrlHelp")}
              stringify
            />
            {useJwksUrl !== "true" &&
              (keyInfo ? (
                <Certificate plain keyInfo={keyInfo} />
              ) : (
                "No client certificate configured"
              ))}
            {useJwksUrl === "true" && (
              <TextControl
                name={convertAttributeNameToForm("attributes.jwks.url")}
                label={t("jwksUrl")}
                labelIcon={t("jwksUrlHelp")}
                type="url"
              />
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
