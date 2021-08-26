import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
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

type JavaKeystoreFormProps = {
  handleModalToggle?: () => void;
  refresh?: () => void;
  editMode?: boolean;
  providerType?: string;
};

export interface MatchParams {
  providerType: string;
}

export const JavaKeystoreForm = ({
  editMode,
  providerType,
  handleModalToggle,
  refresh,
}: JavaKeystoreFormProps) => {
  const { t } = useTranslation("realm-settings");
  const serverInfo = useServerInfo();
  const [isAlgorithmDropdownOpen, setIsAlgorithmDropdownOpen] = useState(false);

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
    Object.entries(component).map(([key, value]) => {
      if (
        key === "config" &&
        component.config?.secretSize &&
        component.config?.active &&
        component.config?.algorithm &&
        component.config?.keystore &&
        component.config?.keystorePassword &&
        component.config?.keyAlias &&
        component.config?.keyPassword
      ) {
        form.setValue("config.secretSize", value.secretSize[0]);

        form.setValue("config.active", value.active[0]);

        form.setValue("config.algorithm", value.algorithm[0]);

        form.setValue("config.keystore", value.keystore[0]);

        form.setValue("config.keyAlias", value.keyAlias[0]);

        form.setValue("config.keyPassword", value.keyPassword[0]);

        convertToFormValues(value, "config", form.setValue);
      }
      form.setValue(key, value);
    });
  };

  useFetch(
    async () => {
      if (editMode) return await adminClient.components.findOne({ id: id });
    },
    (result) => {
      if (result) {
        setupForm(result);
      }
    },
    []
  );

  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const javaKeystoreAlgorithmOptions =
    allComponentTypes[3].properties[3].options;

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
              forLabel={t("common:name")}
              forID={t("common:helpLabel", { label: t("common:name") })}
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
            forLabel={t("common:name")}
            forID={t("common:helpLabel", { label: t("common:name") })}
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
            forLabel={t("enabled")}
            forID={t("common:helpLabel", { label: t("enabled") })}
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
            forLabel={t("active")}
            forID={t("common:helpLabel", { label: t("active") })}
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
            forLabel={t("algorithm")}
            forID={t("common:helpLabel", { label: t("algorithm") })}
          />
        }
      >
        <Controller
          name="config.algorithm"
          control={form.control}
          defaultValue={["RS256"]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-elliptic"
              onToggle={(isExpanded) => setIsAlgorithmDropdownOpen(isExpanded)}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                setIsAlgorithmDropdownOpen(false);
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("algorithm")}
              isOpen={isAlgorithmDropdownOpen}
              placeholderText="Select one..."
              data-testid="select-algorithm"
            >
              {javaKeystoreAlgorithmOptions!.map((p, idx) => (
                <SelectOption
                  selected={p === value}
                  key={`algorithm-${idx}`}
                  value={p}
                ></SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keystore")}
        fieldId="kc-keystore"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keystore"
            forLabel={t("keystore")}
            forID="kc-keystore"
          />
        }
      >
        <Controller
          name="config.keystore"
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keystore")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="select-display-name"
            ></TextInput>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keystorePassword")}
        fieldId="kc-keystore-password"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keystorePassword"
            forLabel={t("keystorePassword")}
            forID={t("common:helpLabel", { label: t("keystorePassword") })}
          />
        }
      >
        <Controller
          name="config.keystorePassword"
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("consoleDisplayName")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="select-display-name"
            ></TextInput>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keyAlias")}
        fieldId="kc-key-alias"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keyAlias"
            forLabel={t("keyAlias")}
            forID={t("common:helpLabel", { label: t("keyAlias") })}
          />
        }
      >
        <Controller
          name="config.keyAlias"
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keyAlias")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="key-alias"
            ></TextInput>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keyPassword")}
        fieldId="kc-key-password"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keyPassword"
            forLabel={t("keyPassword")}
            forID={t("common:helpLabel", { label: t("keyPassword") })}
          />
        }
      >
        <Controller
          name="config.keyPassword"
          control={form.control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keyPassword")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="key-password"
            ></TextInput>
          )}
        />
      </FormGroup>
      <ActionGroup className="kc-java-keystore-form-buttons">
        <Button
          className="kc-java-keystore-form-save-button"
          data-testid="add-provider-button"
          variant="primary"
          type="submit"
        >
          {t("common:save")}
        </Button>
        <Button
          className="kc-java-keystore-form-cancel-button"
          onClick={(!editMode && handleModalToggle) || undefined}
          variant="link"
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};

export const JavaKeystoreSettings = () => {
  const { t } = useTranslation("realm-settings");
  const providerId = useRouteMatch<MatchParams>(
    "/:realm/realm-settings/keys/:id?/:providerType?/settings"
  )?.params.providerType;
  return (
    <>
      <ViewHeader titleKey={t("editProvider")} subKey={providerId} />
      <PageSection variant="light">
        <JavaKeystoreForm providerType={providerId} editMode />
      </PageSection>
    </>
  );
};
