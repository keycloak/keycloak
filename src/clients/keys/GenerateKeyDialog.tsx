import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Control, Controller, useForm } from "react-hook-form";
import {
  Button,
  ButtonVariant,
  FileUpload,
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

type GenerateKeyDialogProps = {
  toggleDialog: () => void;
  save: (keyStoreConfig: KeyStoreConfig) => void;
};

type KeyFormProps = {
  register: () => void;
  control: Control<KeyStoreConfig>;
  useFile?: boolean;
};

export const KeyForm = ({
  register,
  control,
  useFile = false,
}: KeyFormProps) => {
  const { t } = useTranslation("clients");

  const [filename, setFilename] = useState<string>();
  const [openArchiveFormat, setOpenArchiveFormat] = useState(false);

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
          render={({ onChange, value }) => (
            <Select
              toggleId="archiveFormat"
              onToggle={setOpenArchiveFormat}
              onSelect={(_, value) => {
                onChange(value.toString());
                setOpenArchiveFormat(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label={t("archiveFormat")}
              isOpen={openArchiveFormat}
            >
              {["JKS", "PKCS12"].map((option) => (
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
            render={({ onChange, value }) => (
              <FileUpload
                id="importFile"
                value={value}
                filename={filename}
                browseButtonText={t("browse")}
                onChange={(value, filename) => {
                  setFilename(filename);
                  onChange(value);
                }}
              />
            )}
          />
        </FormGroup>
      )}
      <StoreSettings register={register} hidePassword={useFile} />
    </Form>
  );
};

export const GenerateKeyDialog = ({
  save,
  toggleDialog,
}: GenerateKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const { register, control, handleSubmit } = useForm<KeyStoreConfig>();

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
      <KeyForm register={register} control={control} />
    </Modal>
  );
};
