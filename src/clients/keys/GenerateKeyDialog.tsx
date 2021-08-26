import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
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

type GenerateKeyDialogProps = {
  toggleDialog: () => void;
  save: (keyStoreConfig: KeyStoreConfig) => void;
};

export const GenerateKeyDialog = ({
  save,
  toggleDialog,
}: GenerateKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const { register, control, handleSubmit } = useForm<KeyStoreConfig>();

  const [openArchiveFormat, setOpenArchiveFormat] = useState(false);

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
      <Form className="pf-u-pt-lg">
        <FormGroup
          label={t("archiveFormat")}
          labelIcon={
            <HelpItem
              helpText="clients-help:archiveFormat"
              forLabel={t("archiveFormat")}
              forID="archiveFormat"
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
                onToggle={() => setOpenArchiveFormat(!openArchiveFormat)}
                onSelect={(_, value) => {
                  onChange(value as string);
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
        <StoreSettings register={register} />
      </Form>
    </Modal>
  );
};
