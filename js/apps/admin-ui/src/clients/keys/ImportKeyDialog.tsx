import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { HelpItem } from "ui-shared";
import { StoreSettings } from "./StoreSettings";
import { FileUpload } from "../../components/json-file-upload/patternfly/FileUpload";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

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
  const { control, handleSubmit } = form;

  const [openArchiveFormat, setOpenArchiveFormat] = useState(false);

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
          onClick={() => {
            toggleDialog();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <TextContent>
        <Text>{t("generateKeysDescription")}</Text>
      </TextContent>
      <Form className="pf-u-pt-lg">
        <FormGroup
          label={t("archiveFormat")}
          labelIcon={
            <HelpItem
              helpText={t("archiveFormatHelp")}
              fieldLabelId="archiveFormat"
            />
          }
          fieldId="archiveFormat"
        >
          <Controller
            name="keystoreFormat"
            control={control}
            defaultValue={formats[0]}
            render={({ field }) => (
              <Select
                toggleId="archiveFormat"
                onToggle={setOpenArchiveFormat}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setOpenArchiveFormat(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("archiveFormat")}
                isOpen={openArchiveFormat}
              >
                {formats.map((option) => (
                  <SelectOption
                    selected={option === field.value}
                    key={option}
                    value={option}
                  />
                ))}
              </Select>
            )}
          />
        </FormGroup>
        {baseFormats.includes(format) && (
          <FormProvider {...form}>
            <StoreSettings hidePassword />
          </FormProvider>
        )}
        <FormGroup label={t("importFile")} fieldId="importFile">
          <Controller
            name="file"
            control={control}
            defaultValue={{ filename: "" }}
            render={({ field }) => (
              <FileUpload
                id="importFile"
                value={field.value.value}
                filename={field.value.filename}
                onChange={(value, filename) =>
                  field.onChange({ value, filename })
                }
              />
            )}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
