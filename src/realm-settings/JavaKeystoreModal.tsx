import React, { useState } from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
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
import { KEY_PROVIDER_TYPE } from "../util";

type JavaKeystoreModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const JavaKeystoreModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: // save,
JavaKeystoreModalProps) => {
  const { t } = useTranslation("groups");
  const serverInfo = useServerInfo();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { handleSubmit, control } = useForm({});
  const [isEllipticCurveDropdownOpen, setIsEllipticCurveDropdownOpen] =
    useState(false);

  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const save = async (component: ComponentRepresentation) => {
    try {
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
    } catch (error) {
      addError("groups:saveProviderError", error);
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
        {providerType === "java-keystore" && (
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
                name="config.algorithm"
                control={control}
                defaultValue={["RS256"]}
                render={({ onChange, value }) => (
                  <Select
                    toggleId="kc-elliptic"
                    onToggle={() =>
                      setIsEllipticCurveDropdownOpen(
                        !isEllipticCurveDropdownOpen
                      )
                    }
                    onSelect={(_, value) => {
                      onChange([value + ""]);
                      setIsEllipticCurveDropdownOpen(false);
                    }}
                    selections={[value + ""]}
                    variant={SelectVariant.single}
                    aria-label={t("algorithm")}
                    isOpen={isEllipticCurveDropdownOpen}
                    placeholderText="Select one..."
                    data-testid="select-algorithm"
                  >
                    {allComponentTypes[3].properties[3].options!.map(
                      (p, idx) => (
                        <SelectOption
                          selected={p === value}
                          key={`algorithm-${idx}`}
                          value={p}
                        ></SelectOption>
                      )
                    )}
                  </Select>
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("keystore")}
              fieldId="kc-login-theme"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:keystore"
                  fieldLabelId="realm-settings:keystore"
                />
              }
            >
              <Controller
                name="config.keystore"
                control={control}
                defaultValue={[]}
                render={({ onChange }) => (
                  <TextInput
                    aria-label={t("keystore")}
                    onChange={(value) => {
                      onChange([value + ""]);
                    }}
                    data-testid="select-display-name"
                  ></TextInput>
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("keystorePassword")}
              fieldId="kc-login-theme"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:keystorePassword"
                  fieldLabelId="realm-settings:keystorePassword"
                />
              }
            >
              <Controller
                name="config.keystorePassword"
                control={control}
                defaultValue={[]}
                render={({ onChange }) => (
                  <TextInput
                    aria-label={t("consoleDisplayName")}
                    onChange={(value) => {
                      onChange([value + ""]);
                    }}
                    data-testid="select-display-name"
                  ></TextInput>
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("keyAlias")}
              fieldId="kc-login-theme"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:keyAlias"
                  fieldLabelId="realm-settings:keyAlias"
                />
              }
            >
              <Controller
                name="config.keyAlias"
                control={control}
                defaultValue={[]}
                render={({ onChange }) => (
                  <TextInput
                    aria-label={t("consoleDisplayName")}
                    onChange={(value) => {
                      onChange([value + ""]);
                    }}
                    data-testid="select-display-name"
                  ></TextInput>
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("keyPassword")}
              fieldId="kc-login-theme"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:keyPassword"
                  fieldLabelId="realm-settings:keyPassword"
                />
              }
            >
              <Controller
                name="config.keyPassword"
                control={control}
                defaultValue={[]}
                render={({ onChange }) => (
                  <TextInput
                    aria-label={t("consoleDisplayName")}
                    onChange={(value) => {
                      onChange([value + ""]);
                    }}
                    data-testid="select-display-name"
                  ></TextInput>
                )}
              />
            </FormGroup>
          </>
        )}
      </Form>
    </Modal>
  );
};
