// @ts-nocheck
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { isEqual } from "lodash-es";
import { useState } from "react";
import { Controller, UseFormReturn, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";

export type KerberosSettingsRequiredProps = {
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const KerberosSettingsRequired = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: KerberosSettingsRequiredProps) => {
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);

  const allowPassAuth = useWatch({
    control: form.control,
    name: "config.allowPasswordAuthentication",
  });

  useFetch(
    () => adminClient.realms.findOne({ realm }),
    (result) => form.setValue("parentId", result!.id),
    []
  );

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("requiredSettings")}
          description={helpText("kerberosRequiredSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      {/* Required settings */}
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("uiDisplayName")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:uiDisplayNameHelp")}
              fieldLabelId="user-federation:uiDisplayName"
            />
          }
          fieldId="kc-ui-display-name"
          isRequired
          validated={form.formState.errors.name ? "error" : "default"}
          helperTextInvalid={form.formState.errors.name?.message}
        >
          {/* These hidden fields are required so data object written back matches data retrieved */}
          <KeycloakTextInput
            hidden
            id="kc-ui-providerId"
            defaultValue="kerberos"
            {...form.register("providerId")}
          />
          <KeycloakTextInput
            hidden
            id="kc-ui-providerType"
            defaultValue="org.keycloak.storage.UserStorageProvider"
            {...form.register("providerType")}
          />
          <KeycloakTextInput
            hidden
            id="kc-ui-parentId"
            defaultValue={realm}
            {...form.register("parentId")}
          />

          <KeycloakTextInput
            isRequired
            id="kc-ui-name"
            data-testid="kerberos-name"
            validated={form.formState.errors.name ? "error" : "default"}
            aria-label={t("uiDisplayName")}
            {...form.register("name", {
              required: {
                value: true,
                message: t("validateName"),
              },
            })}
          />
        </FormGroup>

        <FormGroup
          label={t("kerberosRealm")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:kerberosRealmHelp")}
              fieldLabelId="user-federation:kc-kerberos-realm"
            />
          }
          fieldId="kc-kerberos-realm"
          isRequired
          validated={
            form.formState.errors.config?.kerberosRealm?.[0]
              ? "error"
              : "default"
          }
          helperTextInvalid={
            form.formState.errors.config?.kerberosRealm?.[0].message
          }
        >
          <KeycloakTextInput
            isRequired
            id="kc-kerberos-realm"
            data-testid="kerberos-realm"
            validated={
              form.formState.errors.config?.kerberosRealm?.[0]
                ? "error"
                : "default"
            }
            {...form.register("config.kerberosRealm.0", {
              required: {
                value: true,
                message: t("validateRealm"),
              },
            })}
          />
        </FormGroup>

        <FormGroup
          label={t("serverPrincipal")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:serverPrincipalHelp")}
              fieldLabelId="user-federation:serverPrincipal"
            />
          }
          fieldId="kc-server-principal"
          isRequired
          validated={
            form.formState.errors.config?.serverPrincipal?.[0]
              ? "error"
              : "default"
          }
          helperTextInvalid={
            form.formState.errors.config?.serverPrincipal?.[0].message
          }
        >
          <KeycloakTextInput
            isRequired
            id="kc-server-principal"
            data-testid="kerberos-principal"
            validated={
              form.formState.errors.config?.serverPrincipal?.[0]
                ? "error"
                : "default"
            }
            {...form.register("config.serverPrincipal.0", {
              required: {
                value: true,
                message: t("validateServerPrincipal"),
              },
            })}
          />
        </FormGroup>

        <FormGroup
          label={t("keyTab")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:keyTabHelp")}
              fieldLabelId="user-federation:keyTab"
            />
          }
          fieldId="kc-key-tab"
          isRequired
          validated={
            form.formState.errors.config?.keyTab?.[0] ? "error" : "default"
          }
          helperTextInvalid={form.formState.errors.config?.keyTab?.[0].message}
        >
          <KeycloakTextInput
            isRequired
            id="kc-key-tab"
            data-testid="kerberos-keytab"
            validated={
              form.formState.errors.config?.keyTab?.[0] ? "error" : "default"
            }
            {...form.register("config.keyTab.0", {
              required: {
                value: true,
                message: t("validateKeyTab"),
              },
            })}
          />
        </FormGroup>

        <FormGroup
          label={t("debug")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:debugHelp")}
              fieldLabelId="user-federation:debug"
            />
          }
          fieldId="kc-debug"
          hasNoPaddingTop
        >
          {" "}
          <Controller
            name="config.debug"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-debug"}
                data-testid="debug"
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("debug")}
              />
            )}
          />
        </FormGroup>

        <FormGroup
          label={t("allowPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t(
                "user-federation-help:allowPasswordAuthenticationHelp"
              )}
              fieldLabelId="user-federation:allowPasswordAuthentication"
            />
          }
          fieldId="kc-allow-password-authentication"
          hasNoPaddingTop
        >
          <Controller
            name="config.allowPasswordAuthentication"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-allow-password-authentication"}
                data-testid="allow-password-authentication"
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("allowPasswordAuthentication")}
              />
            )}
          />
        </FormGroup>

        {isEqual(allowPassAuth, ["true"]) ? (
          <FormGroup
            label={t("editMode")}
            labelIcon={
              <HelpItem
                helpText={t("user-federation-help:editModeKerberosHelp")}
                fieldLabelId="user-federation:editMode"
              />
            }
            isRequired
            fieldId="kc-edit-mode"
          >
            {" "}
            <Controller
              name="config.editMode[0]"
              defaultValue="READ_ONLY"
              control={form.control}
              rules={{ required: true }}
              render={({ field }) => (
                <Select
                  toggleId="kc-edit-mode"
                  required
                  onToggle={() =>
                    setIsEditModeDropdownOpen(!isEditModeDropdownOpen)
                  }
                  isOpen={isEditModeDropdownOpen}
                  onSelect={(_, value) => {
                    field.onChange(value as string);
                    setIsEditModeDropdownOpen(false);
                  }}
                  selections={field.value}
                  variant={SelectVariant.single}
                >
                  <SelectOption key={0} value="READ_ONLY" isPlaceholder />
                  <SelectOption key={1} value="UNSYNCED" />
                </Select>
              )}
            ></Controller>
          </FormGroup>
        ) : null}

        <FormGroup
          label={t("updateFirstLogin")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:updateFirstLoginHelp")}
              fieldLabelId="user-federation:updateFirstLogin"
            />
          }
          fieldId="kc-update-first-login"
          hasNoPaddingTop
        >
          <Controller
            name="config.updateProfileFirstLogin"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-update-first-login"}
                data-testid="update-first-login"
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("updateFirstLogin")}
              />
            )}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
