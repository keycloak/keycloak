import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FileUpload,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { useParams, useRouteMatch } from "react-router-dom";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { convertToFormValues, KEY_PROVIDER_TYPE } from "../../../util";
import { useAlerts } from "../../../components/alert/Alerts";

type RSAFormProps = {
  handleModalToggle?: () => void;
  refresh?: () => void;
  editMode?: boolean;
  providerType?: string;
};

export interface MatchParams {
  providerType: string;
}

export const RSAForm = ({
  editMode,
  providerType,
  handleModalToggle,
  refresh,
}: RSAFormProps) => {
  const { t } = useTranslation("realm-settings");
  const serverInfo = useServerInfo();

  const [component, setComponent] = useState<ComponentRepresentation>();

  const [isRSAalgDropdownOpen, setIsRSAalgDropdownOpen] = useState(false);

  const [keyFileName, setKeyFileName] = useState("");
  const [certificateFileName, setCertificateFileName] = useState("");

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { id } = useParams<{ id: string }>();

  const providerId =
    useRouteMatch<MatchParams>("/:providerType?")?.params.providerType;

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
        handleModalToggle?.();
        addAlert(t("saveProviderSuccess"), AlertVariant.success);
        refresh?.();
      }
    } catch (error) {
      addError("realm-settings:saveProviderError", error);
    }
  };

  const form = useForm<ComponentRepresentation>({ mode: "onChange" });

  const setupForm = (component: ComponentRepresentation) => {
    form.reset();
    convertToFormValues(component, form.setValue);
  };

  useFetch(
    async () => {
      if (editMode) return await adminClient.components.findOne({ id: id });
    },
    (result) => {
      if (result) {
        setupForm(result);
        setComponent(result);
      }
    },
    []
  );

  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const rsaAlgOptions = allComponentTypes[4].properties[3].options;

  return (
    <FormAccess
      isHorizontal
      id="add-provider"
      className="pf-u-mt-lg"
      role="manage-realm"
      onSubmit={form.handleSubmit(save)}
    >
      {editMode && (
        <FormGroup
          label={t("providerId")}
          labelIcon={
            <HelpItem
              helpText="client-scopes-help:mapperName"
              fieldLabelId="realm-settings:providerId"
            />
          }
          fieldId="id"
          isRequired
          validated={
            form.errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          helperTextInvalid={t("common:required")}
        >
          <TextInput
            ref={form.register()}
            id="id"
            type="text"
            name="id"
            isReadOnly={editMode}
            aria-label={t("consoleDisplayName")}
            defaultValue={id}
            data-testid="display-name-input"
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:mapperName"
            fieldLabelId="name"
          />
        }
        fieldId="name"
        isRequired
        validated={
          form.errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        {!editMode && (
          <Controller
            name="name"
            control={form.control}
            defaultValue={providerType}
            render={({ onChange, value }) => {
              return (
                <TextInput
                  id="name"
                  type="text"
                  aria-label={t("consoleDisplayName")}
                  defaultValue={providerType}
                  value={value}
                  onChange={(value) => onChange(value)}
                  data-testid="display-name-input"
                />
              );
            }}
          />
        )}
        {editMode && (
          <TextInput
            ref={form.register()}
            type="text"
            id="name"
            name="name"
            defaultValue={providerId}
            validated={
              form.errors.name
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        )}
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
          control={form.control}
          defaultValue={["true"]}
          render={({ onChange, value }) => (
            <Switch
              id="kc-enabled-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value[0] === "true"}
              data-testid={value[0] === "true" ? "enabled" : "disabled"}
              onChange={(value) => {
                onChange([value.toString()]);
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
            fieldLabelId="active"
          />
        }
      >
        <Controller
          name="config.active"
          control={form.control}
          defaultValue={["true"]}
          render={({ onChange, value }) => {
            return (
              <Switch
                id="kc-active-switch"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value[0] === "true"}
                data-testid={value[0] === "true" ? "active" : "passive"}
                onChange={(value) => {
                  onChange([value.toString()]);
                }}
              />
            );
          }}
        />
      </FormGroup>
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
          defaultValue={["RS256"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-rsa-algorithm"
              onToggle={() => setIsRSAalgDropdownOpen(!isRSAalgDropdownOpen)}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                setIsRSAalgDropdownOpen(false);
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("algorithm")}
              isOpen={isRSAalgDropdownOpen}
              data-testid="select-rsa-algorithm"
            >
              {rsaAlgOptions!.map((p, idx) => (
                <SelectOption
                  selected={p === value}
                  key={`rsa-algorithm-${idx}`}
                  value={p}
                ></SelectOption>
              ))}
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
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <FileUpload
              id="importPrivateKey"
              type="text"
              value={component?.config?.privateKey[0]}
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
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <FileUpload
              id="importCertificate"
              type="text"
              value={component?.config?.certificate[0]}
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
      <ActionGroup className="kc-hmac-form-buttons">
        <Button
          className="kc-hmac-form-save-button"
          data-testid="add-provider-button"
          variant="primary"
          type="submit"
        >
          {t("common:save")}
        </Button>
        <Button
          className="kc-hmac-form-cancel-button"
          onClick={(!editMode && handleModalToggle) || undefined}
          variant="link"
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};

export default function RSASettings() {
  const { t } = useTranslation("realm-settings");
  const providerId = useRouteMatch<MatchParams>(
    "/:realm/realm-settings/keys/:id?/:providerType?/settings"
  )?.params.providerType;
  return (
    <>
      <ViewHeader titleKey={t("editProvider")} subKey={providerId} />
      <PageSection variant="light">
        <RSAForm providerType={providerId} editMode />
      </PageSection>
    </>
  );
}
