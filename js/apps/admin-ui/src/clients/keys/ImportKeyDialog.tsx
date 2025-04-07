import { SelectControl } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  FileUpload,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
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
  file: { value?: string; filename: string };
};

export const ImportKeyDialog = ({
  save,
  toggleDialog,
}: ImportKeyDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<ImportFile>();
  const [file, setFile] = useState<string>("");
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
      <TextContent>
        <Text>{t("generateKeysDescription")}</Text>
      </TextContent>
      <Form className="pf-v5-u-pt-lg">
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
          {baseFormats.includes(format) && <StoreSettings hidePassword />}
          <FormGroup label={t("importFile")} fieldId="importFile">
            <Controller
              name="file"
              control={control}
              defaultValue={{ value: "", filename: "" }}
              render={({ field }) => (
                <FileUpload
                  id="importFile"
                  value={field.value.value}
                  filename={file}
                  hideDefaultPreview
                  type="text"
                  onDataChange={(_, value) => {
                    field.onChange({
                      value,
                    });
                  }}
                  onFileInputChange={(_, file) => {
                    setFile(file.name);
                  }}
                />
              )}
            />
          </FormGroup>
        </FormProvider>
      </Form>
    </Modal>
  );
};
