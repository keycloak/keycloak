import { FileUploadControl, SelectControl } from "@keycloak/keycloak-ui-shared";
import { Button, ButtonVariant, Content, Form } from "@patternfly/react-core";
import { Modal, ModalVariant } from "@patternfly/react-core/deprecated";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { StoreSettings } from "./StoreSettings";

type ImportKeyDialogProps = {
  toggleDialog: () => void;
  save: (importFile: ImportFile) => void;
};

export type ImportFile = {
  keystoreFormat: string;
  keyAlias: string;
  storePassword: string;
  file: File | string;
};

export const ImportKeyDialog = ({
  save,
  toggleDialog,
}: ImportKeyDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<ImportFile>();
  const { control, handleSubmit } = form;

  const baseFormats = useServerInfo().cryptoInfo?.supportedKeystoreTypes ?? [];

  const formats = baseFormats.concat([
    "Certificate PEM",
    "Public Key PEM",
    "JSON Web Key Set",
  ]);

  const format = useWatch({
    control,
    name: "keystoreFormat",
    defaultValue: formats[0],
  });

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("generateKeys")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          id="modal-confirm"
          data-testid="confirm"
          key="confirm"
          onClick={() => {
            handleSubmit((importFile) => {
              save(importFile);
              toggleDialog();
            })();
          }}
        >
          {t("import")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Content>
        <Content component="p">{t("generateKeysDescription")}</Content>
      </Content>
      <Form className="pf-v6-u-pt-lg">
        <FormProvider {...form}>
          <SelectControl
            name="keystoreFormat"
            label={t("archiveFormat")}
            labelIcon={t("archiveFormatHelp")}
            controller={{
              defaultValue: formats[0],
            }}
            options={formats}
          />
          <FileUploadControl
            label={t("importFile")}
            id="importFile"
            name="file"
            rules={{
              required: t("required"),
            }}
          />
          {baseFormats.includes(format) && <StoreSettings hidePassword />}
        </FormProvider>
      </Form>
    </Modal>
  );
};
