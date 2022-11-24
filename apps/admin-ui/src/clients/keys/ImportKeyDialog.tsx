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
import { HelpItem } from "../../components/help-enabler/HelpItem";
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
  file: { value: File; filename: string };
};

export const ImportKeyDialog = ({
  save,
  toggleDialog,
}: ImportKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const form = useForm<ImportFile>();
  const { control, handleSubmit } = form;

  const [openArchiveFormat, setOpenArchiveFormat] = useState(false);

  const format = useWatch<string>({
    control,
    name: "keystoreFormat",
    defaultValue: "JKS",
  });

  const baseFormats = useServerInfo().cryptoInfo?.supportedKeystoreTypes ?? [];

  const formats = baseFormats.concat([
    "Certificate PEM",
    "Public Key PEM",
    "JSON Web Key Set",
  ]);

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
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <TextContent>
        <Text>{t("clients-help:generateKeysDescription")}</Text>
      </TextContent>
      <Form className="pf-u-pt-lg">
        <FormGroup
          label={t("archiveFormat")}
          labelIcon={
            <HelpItem
              helpText="clients-help:archiveFormat"
              fieldLabelId="clients:archiveFormat"
            />
          }
          fieldId="archiveFormat"
        >
          <Controller
            name="keystoreFormat"
            control={control}
            defaultValue="JKS"
            render={({ onChange, value }) => (
              <Select
                toggleId="archiveFormat"
                onToggle={setOpenArchiveFormat}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setOpenArchiveFormat(false);
                }}
                selections={value}
                variant={SelectVariant.single}
                aria-label={t("archiveFormat")}
                isOpen={openArchiveFormat}
              >
                {formats.map((option) => (
                  <SelectOption
                    selected={option === value}
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
            defaultValue=""
            render={({ onChange, value }) => (
              <FileUpload
                id="importFile"
                value={value.value}
                filename={value.filename}
                onChange={(value, filename) => onChange({ value, filename })}
              />
            )}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
