import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import { Button, Modal, Form } from "@patternfly/react-core";
import FileSaver from "file-saver";

import KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { KeyForm } from "./GenerateKeyDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";

type ExportSamlKeyDialogProps = {
  clientId: string;
  close: () => void;
};

export const ExportSamlKeyDialog = ({
  clientId,
  close,
}: ExportSamlKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const { realm } = useRealm();

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const form = useForm<KeyStoreConfig>({
    defaultValues: { realmAlias: realm },
  });

  const download = async (config: KeyStoreConfig) => {
    try {
      const keyStore = await adminClient.clients.downloadKey(
        {
          id: clientId,
          attr: "saml.signing",
        },
        config
      );
      FileSaver.saveAs(
        new Blob([keyStore], { type: "application/octet-stream" }),
        `keystore.${config.format == "PKCS12" ? "p12" : "jks"}`
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
