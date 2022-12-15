import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
} from "react-hook-form-v7";
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

import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { StoreSettings } from "./StoreSettings";
import { FileUpload } from "../../components/json-file-upload/patternfly/FileUpload";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

type GenerateKeyDialogProps = {
  clientId: string;
  toggleDialog: () => void;
  save: (keyStoreConfig: KeyStoreConfig) => void;
};

type KeyFormProps = {
  useFile?: boolean;
  isSaml?: boolean;
  hasPem?: boolean;
};

const CERT_PEM = "Certificate PEM" as const;

const extensions = new Map([
  ["PKCS12", "p12"],
  ["JKS", "jks"],
  ["BCFKS", "bcfks"],
]);

type FormFields = KeyStoreConfig & {
  file: string | File;
};

export const getFileExtension = (format: string) => extensions.get(format);

export const KeyForm = ({
  isSaml = false,
  hasPem = false,
  useFile = false,
}: KeyFormProps) => {
  const { t } = useTranslation("clients");

  const [filename, setFilename] = useState<string>();
  const [openArchiveFormat, setOpenArchiveFormat] = useState(false);

  const { control, watch } = useFormContext<FormFields>();
  const format = watch("format");

  const { cryptoInfo } = useServerInfo();

  return (
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
          name="format"
          defaultValue="JKS"
          control={control}
          render={({ field }) => (
            <Select
              toggleId="archiveFormat"
              onToggle={setOpenArchiveFormat}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setOpenArchiveFormat(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("archiveFormat")}
              isOpen={openArchiveFormat}
            >
              {cryptoInfo?.supportedKeystoreTypes
                .concat(hasPem ? CERT_PEM : [])
                .map((option) => (
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
      {useFile && (
        <FormGroup
          label={t("importFile")}
          labelIcon={
            <HelpItem
              helpText="clients-help:importFile"
              fieldLabelId="clients:importFile"
            />
          }
          fieldId="importFile"
        >
          <Controller
            name="file"
            defaultValue=""
            control={control}
            render={({ field }) => (
              <FileUpload
                id="importFile"
                value={field.value}
                filename={filename}
                browseButtonText={t("browse")}
                onChange={(value, filename) => {
                  setFilename(filename);
                  field.onChange(value);
                }}
              />
            )}
          />
        </FormGroup>
      )}
      {format !== CERT_PEM && (
        <StoreSettings hidePassword={useFile} isSaml={isSaml} />
      )}
    </Form>
  );
};

export const GenerateKeyDialog = ({
  clientId,
  save,
  toggleDialog,
}: GenerateKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const form = useForm<KeyStoreConfig>({
    defaultValues: { keyAlias: clientId },
    mode: "onChange",
  });

  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("generateKeys")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          data-testid="confirm"
          isDisabled={!isValid}
          onClick={() => {
            handleSubmit((config) => {
              save(config);
              toggleDialog();
            })();
          }}
        >
          {t("generate")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          data-testid="cancel"
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
      <FormProvider {...form}>
        <KeyForm />
      </FormProvider>
    </Modal>
  );
};
