import { useState } from "react";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { UseFormMethods, Controller, useWatch } from "react-hook-form";

import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { isEqual } from "lodash-es";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

export type KerberosSettingsRequiredProps = {
  form: UseFormMethods;
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
              helpText="user-federation-help:uiDisplayNameHelp"
              fieldLabelId="user-federation:uiDisplayName"
            />
          }
          fieldId="kc-ui-display-name"
          isRequired
          validated={form.errors.name ? "error" : "default"}
          helperTextInvalid={form.errors.name?.message}
        >
          {/* These hidden fields are required so data object written back matches data retrieved */}
          <KeycloakTextInput
            hidden
            type="text"
            id="kc-ui-providerId"
            name="providerId"
            defaultValue="kerberos"
            ref={form.register}
            aria-label={t("providerId")}
          />
          <KeycloakTextInput
            hidden
            type="text"
            id="kc-ui-providerType"
            name="providerType"
            defaultValue="org.keycloak.storage.UserStorageProvider"
            ref={form.register}
            aria-label={t("providerType")}
          />
          <KeycloakTextInput
            hidden
            type="text"
            id="kc-ui-parentId"
            name="parentId"
            defaultValue={realm}
            ref={form.register}
            aria-label={t("parentId")}
          />

          <KeycloakTextInput
            isRequired
            type="text"
            id="kc-ui-name"
            name="name"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateName")}`,
              },
            })}
            data-testid="kerberos-name"
            validated={form.errors.name ? "error" : "default"}
            aria-label={t("uiDisplayName")}
          />
        </FormGroup>

        <FormGroup
          label={t("kerberosRealm")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:kerberosRealmHelp"
              fieldLabelId="user-federation:kc-kerberos-realm"
            />
          }
          fieldId="kc-kerberos-realm"
          isRequired
          validated={
            form.errors.config?.kerberosRealm?.[0] ? "error" : "default"
          }
          helperTextInvalid={form.errors.config?.kerberosRealm?.[0].message}
        >
          <KeycloakTextInput
            isRequired
            type="text"
            id="kc-kerberos-realm"
            name="config.kerberosRealm[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateRealm")}`,
              },
            })}
            data-testid="kerberos-realm"
            aria-label={t("kerberosRealm")}
            validated={
              form.errors.config?.kerberosRealm?.[0] ? "error" : "default"
            }
          />
        </FormGroup>

        <FormGroup
          label={t("serverPrincipal")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:serverPrincipalHelp"
              fieldLabelId="user-federation:serverPrincipal"
            />
          }
          fieldId="kc-server-principal"
          isRequired
          validated={
            form.errors.config?.serverPrincipal?.[0] ? "error" : "default"
          }
          helperTextInvalid={form.errors.config?.serverPrincipal?.[0].message}
        >
          <KeycloakTextInput
            isRequired
            type="text"
            id="kc-server-principal"
            name="config.serverPrincipal[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateServerPrincipal")}`,
              },
            })}
            data-testid="kerberos-principal"
            aria-label={t("kerberosPrincipal")}
            validated={
              form.errors.config?.serverPrincipal?.[0] ? "error" : "default"
            }
          />
        </FormGroup>

        <FormGroup
          label={t("keyTab")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:keyTabHelp"
              fieldLabelId="user-federation:keyTab"
            />
          }
          fieldId="kc-key-tab"
          isRequired
          validated={form.errors.config?.keyTab?.[0] ? "error" : "default"}
          helperTextInvalid={form.errors.config?.keyTab?.[0].message}
        >
          <KeycloakTextInput
            isRequired
            type="text"
            id="kc-key-tab"
            name="config.keyTab[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateKeyTab")}`,
              },
            })}
            data-testid="kerberos-keytab"
            aria-label={t("kerberosKeyTab")}
            validated={form.errors.config?.keyTab?.[0] ? "error" : "default"}
          />
        </FormGroup>

        <FormGroup
          label={t("debug")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:debugHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id={"kc-debug"}
                data-testid="debug"
                onChange={(value) => onChange([`${value}`])}
                isChecked={value?.[0] === "true"}
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
              helpText="user-federation-help:allowPasswordAuthenticationHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id={"kc-allow-password-authentication"}
                data-testid="allow-password-authentication"
                onChange={(value) => onChange([`${value}`])}
                isChecked={value?.[0] === "true"}
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
                helpText="user-federation-help:editModeKerberosHelp"
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
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-edit-mode"
                  required
                  onToggle={() =>
                    setIsEditModeDropdownOpen(!isEditModeDropdownOpen)
                  }
                  isOpen={isEditModeDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setIsEditModeDropdownOpen(false);
                  }}
                  selections={value}
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
              helpText="user-federation-help:updateFirstLoginHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id={"kc-update-first-login"}
                data-testid="update-first-login"
                onChange={(value) => onChange([`${value}`])}
                isChecked={value?.[0] === "true"}
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
