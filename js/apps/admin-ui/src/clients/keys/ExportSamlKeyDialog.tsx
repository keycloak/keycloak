import KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { Button, Form, Modal } from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useRealm } from "../../context/realm-context/RealmContext";
import { KeyForm, getFileExtension } from "./GenerateKeyDialog";
import { KeyTypes } from "./SamlKeys";

type ExportSamlKeyDialogProps = {
  clientId: string;
  close: () => void;
  keyType: KeyTypes;
};

export const ExportSamlKeyDialog = ({
  clientId,
  close,
  keyType,
}: ExportSamlKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const { realm } = useRealm();

  const { addAlert, addError } = useAlerts();

  const form = useForm<KeyStoreConfig>({
    defaultValues: { realmAlias: realm },
  });

  const download = async (config: KeyStoreConfig) => {
    try {
      const keyStore = await adminClient.clients.downloadKey(
        {
          id: clientId,
          attr: keyType,
        },
        config,
      );
      saveAs(
        new Blob([keyStore], { type: "application/octet-stream" }),
        `keystore.${getFileExtension(config.format ?? "")}`,
      );
      addAlert(t("samlKeysExportSuccess"));
      close();
    } catch (error) {
      addError("clients:samlKeysExportError", error);
    }
  };

  return (
    <Modal
      variant="medium"
      title={t("exportSamlKeyTitle")}
      isOpen
      onClose={close}
      actions={[
        <Button
          id="modal-confirm"
          data-testid="confirm"
          key="confirm"
          type="submit"
          form="export-saml-key-form"
        >
          {t("common:export")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={() => {
            close();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form
        id="export-saml-key-form"
        className="pf-u-pt-lg"
        onSubmit={form.handleSubmit(download)}
      >
        <FormProvider {...form}>
          <KeyForm isSaml />
        </FormProvider>
      </Form>
    </Modal>
  );
};
