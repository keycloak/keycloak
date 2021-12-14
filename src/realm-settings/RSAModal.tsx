import React, { useState } from "react";
import {
  AlertVariant,
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
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";

import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useParams } from "react-router-dom";
import { KEY_PROVIDER_TYPE } from "../util";

type RSAModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const RSAModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: RSAModalProps) => {
  const { t } = useTranslation("realm-settings");
  const serverInfo = useServerInfo();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { handleSubmit, control } = useForm({});
  const [isRSAalgDropdownOpen, setIsRSAalgDropdownOpen] = useState(false);

  const { id } = useParams<{ id: string }>();

  const [keyFileName, setKeyFileName] = useState("");
  const [certificateFileName, setCertificateFileName] = useState("");

  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const save = async (component: ComponentRepresentation) => {
    try {
      if (id) {
        await adminClient.components.update(
          { id },
          {
            ...component,
            parentId: component.parentId,
            providerId: providerType,
            providerType: KEY_PROVIDER_TYPE,
          }
        );
        addAlert(t("saveProviderSuccess"), AlertVariant.success);
      } else {
        await adminClient.components.create({
          ...component,
          parentId: component.parentId,
          providerId: providerType,
          providerType: KEY_PROVIDER_TYPE,
          config: { priority: ["0"] },
        });
        handleModalToggle();
        addAlert(t("saveProviderSuccess"), AlertVariant.success);
        refresh();
      }
    } catch (error) {
      addError("realm-settings:saveProviderError", error);
    }
  };

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen={open}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="add-provider-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="add-provider"
        >
          {t("common:Add")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle!();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form
        isHorizontal
        id="add-provider"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(save!)}
      >
        <FormGroup
          label={t("consoleDisplayName")}
          fieldId="kc-console-display-name"
          labelIcon={
            <HelpItem
              helpText="realm-settings-help:displayName"
              fieldLabelId="realm-settings:consoleDisplayName"
            />
          }
        >
          <Controller
            name="name"
            control={control}
            defaultValue={providerType}
            render={({ onChange }) => (
              <TextInput
                aria-label={t("consoleDisplayName")}
                defaultValue={providerType}
                onChange={(value) => {
                  onChange(value);
                }}
                data-testid="display-name-input"
              ></TextInput>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("common:enabled")}
          fieldId="kc-enabled"
          labelIcon={
            <HelpItem
              helpText={t("realm-settings-help:enabled")}
              fieldLabelId="enabled"
            />
          }
        >
          <Controller
            name="config.enabled"
            control={control}
            defaultValue={["true"]}
            render={({ onChange, value }) => (
              <Switch
                id="kc-enabled"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value[0] === "true"}
                data-testid={
                  value[0] === "true"
                    ? "internationalization-enabled"
                    : "internationalization-disabled"
                }
                onChange={(value) => {
                  onChange([value + ""]);
                }}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("active")}
          fieldId="kc-active"
          labelIcon={
            <HelpItem
              helpText="realm-settings-help:active"
              fieldLabelId="realm-settings:active"
            />
          }
        >
          <Controller
            name="config.active"
            control={control}
            defaultValue={["true"]}
            render={({ onChange, value }) => (
              <Switch
                id="kc-active"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value[0] === "true"}
                data-testid={
                  value[0] === "true"
                    ? "internationalization-enabled"
                    : "internationalization-disabled"
                }
                onChange={(value) => {
                  onChange([value + ""]);
                }}
              />
            )}
          />
        </FormGroup>
        {providerType === "rsa" && (
          <>
            <FormGroup
              label={t("algorithm")}
              fieldId="kc-algorithm"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:algorithm"
                  fieldLabelId="realm-settings:algorithm"
                />
              }
            >
              <Controller
                name="algorithm"
                defaultValue=""
                render={({ onChange, value }) => (
                  <Select
                    toggleId="kc-rsa-algorithm"
                    onToggle={() =>
                      setIsRSAalgDropdownOpen(!isRSAalgDropdownOpen)
                    }
                    onSelect={(_, value) => {
                      onChange(value as string);
                      setIsRSAalgDropdownOpen(false);
                    }}
                    selections={[value + ""]}
                    variant={SelectVariant.single}
                    aria-label={t("algorithm")}
                    isOpen={isRSAalgDropdownOpen}
                    data-testid="select-rsa-algorithm"
                  >
                    {allComponentTypes[4].properties[3].options!.map(
                      (p, idx) => (
                        <SelectOption
                          selected={p === value}
                          key={`rsa-algorithm-${idx}`}
                          value={p}
                        ></SelectOption>
                      )
                    )}
                  </Select>
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("privateRSAKey")}
              fieldId="kc-private-rsa-key"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:privateRSAKey"
                  fieldLabelId="realm-settings:privateRSAKey"
                />
              }
            >
              <Controller
                name="config.privateKey"
                control={control}
                defaultValue={[]}
                render={({ onChange, value }) => (
                  <FileUpload
                    id="importPrivateKey"
                    type="text"
                    value={value[0]}
                    filenamePlaceholder="Upload a PEM file or paste key below"
                    filename={keyFileName}
                    onChange={(value, fileName) => {
                      setKeyFileName(fileName);
                      onChange([value]);
                    }}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("x509Certificate")}
              fieldId="kc-aes-keysize"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:x509Certificate"
                  fieldLabelId="realm-settings:x509Certificate"
                />
              }
            >
              <Controller
                name="config.certificate"
                control={control}
                defaultValue={[]}
                render={({ onChange, value }) => (
                  <FileUpload
                    id="importCertificate"
                    type="text"
                    value={value[0]}
                    filenamePlaceholder="Upload a PEM file or paste key below"
                    filename={certificateFileName}
                    onChange={(value, fileName) => {
                      setCertificateFileName(fileName);
                      onChange([value]);
                    }}
                  />
                )}
              />
            </FormGroup>
          </>
        )}
      </Form>
    </Modal>
  );
};
