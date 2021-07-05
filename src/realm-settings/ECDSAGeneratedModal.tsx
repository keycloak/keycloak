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
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useRealm } from "../context/realm-context/RealmContext";

type ECDSAGeneratedModalProps = {
  providerType?: string;
  handleModalToggle?: () => void;
  refresh?: () => void;
  open: boolean;
};

export const ECDSAGeneratedModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: // save,
ECDSAGeneratedModalProps) => {
  const { t } = useTranslation("groups");
  const serverInfo = useServerInfo();
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { handleSubmit, control } = useForm({});
  const [isEllipticCurveDropdownOpen, setIsEllipticCurveDropdownOpen] =
    useState(false);
  const [isRSAalgDropdownOpen, setIsRSAalgDropdownOpen] = useState(false);
  const [displayName, setDisplayName] = useState("");
  const realm = useRealm();

  const [keyFileName, setKeyFileName] = useState("");
  const [certificateFileName, setCertificateFileName] = useState("");

  const allComponentTypes =
    serverInfo.componentTypes!["org.keycloak.keys.KeyProvider"];

  const save = async (component: ComponentRepresentation) => {
    try {
      await adminClient.components.create({
        parentId: realm.realm,
        name: displayName !== "" ? displayName : providerType,
        providerId: providerType,
        providerType: "org.keycloak.keys.KeyProvider",
        ...component,
      });
      refresh!();
      addAlert(t("realm-settings:saveProviderSuccess"), AlertVariant.success);
      handleModalToggle!();
    } catch (error) {
      addAlert(
        t("realm-settings:saveProviderError") +
          error.response?.data?.errorMessage || error,
        AlertVariant.danger
      );
    }
  };

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("realm-settings:addProvider")}
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
          label={t("realm-settings:consoleDisplayName")}
          fieldId="kc-console-display-name"
          labelIcon={
            <HelpItem
              helpText="realm-settings-help:displayName"
              forLabel={t("loginTheme")}
              forID="kc-console-display-name"
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
                  setDisplayName(value);
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
              forLabel={t("enabled")}
              forID="kc-enabled"
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
          label={t("realm-settings:active")}
          fieldId="kc-active"
          labelIcon={
            <HelpItem
              helpText="realm-settings-help:active"
              forLabel={t("active")}
              forID="kc-active"
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
              label={t("realm-settings:algorithm")}
              fieldId="kc-algorithm"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:algorithm"
                  forLabel={t("algorithm")}
                  forID="kc-algorithm"
                />
              }
            >
              <Controller
                name="algorithm"
                control={control}
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
              label={t("realm-settings:privateRSAKey")}
              fieldId="kc-private-rsa-key"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:privateRSAKey"
                  forLabel={t("privateRSAKey")}
                  forID="kc-rsa-key"
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
              label={t("realm-settings:x509Certificate")}
              fieldId="kc-aes-keysize"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:x509Certificate"
                  forLabel={t("x509Certificate")}
                  forID="kc-x509-certificatw"
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
        {providerType === "ecdsa-generated" && (
          <FormGroup
            label={t("realm-settings:ellipticCurve")}
            fieldId="kc-algorithm"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:ellipticCurve"
                forLabel={t("emailTheme")}
                forID="kc-email-theme"
              />
            }
          >
            <Controller
              name="config.ecdsaEllipticCurveKey"
              control={control}
              defaultValue={["P-256"]}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-elliptic"
                  onToggle={() =>
                    setIsEllipticCurveDropdownOpen(!isEllipticCurveDropdownOpen)
                  }
                  onSelect={(_, value) => {
                    onChange([value + ""]);
                    setIsEllipticCurveDropdownOpen(false);
                  }}
                  selections={[value + ""]}
                  variant={SelectVariant.single}
                  aria-label={t("emailTheme")}
                  isOpen={isEllipticCurveDropdownOpen}
                  placeholderText="Select one..."
                  data-testid="select-email-theme"
                >
                  {allComponentTypes[1].properties[3].options!.map((p, idx) => (
                    <SelectOption
                      selected={p === value}
                      key={`email-theme-${idx}`}
                      value={p}
                    ></SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        )}
      </Form>
    </Modal>
  );
};
