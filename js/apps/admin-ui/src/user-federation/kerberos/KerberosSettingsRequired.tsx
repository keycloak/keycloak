import { FormGroup, Switch } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { isEqual } from "lodash-es";
import { useState } from "react";
import {
  Controller,
  FormProvider,
  UseFormReturn,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "ui-shared";
import { adminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";

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
  const { t } = useTranslation();

  const { realm } = useRealm();

  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);

  const allowPassAuth = useWatch({
    control: form.control,
    name: "config.allowPasswordAuthentication",
  });

  useFetch(
    () => adminClient.realms.findOne({ realm }),
    (result) => form.setValue("parentId", result!.id),
    [],
  );

  return (
    <FormProvider {...form}>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("requiredSettings")}
          description={t("kerberosRequiredSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      {/* Required settings */}
      <FormAccess role="manage-realm" isHorizontal>
        {/* These hidden fields are required so data object written back matches data retrieved */}
        <input
          type="hidden"
          defaultValue="kerberos"
          {...form.register("providerId")}
        />
        <input
          type="hidden"
          defaultValue="org.keycloak.storage.UserStorageProvider"
          {...form.register("providerType")}
        />
        <input
          type="hidden"
          defaultValue={realm}
          {...form.register("parentId")}
        />
        <TextControl
          name="name"
          label={t("uiDisplayName")}
          labelIcon={t("uiDisplayNameHelp")}
          rules={{
            required: t("validateName"),
          }}
        />
        <TextControl
          name="config.kerberosRealm.0"
          label={t("kerberosRealm")}
          labelIcon={t("kerberosRealmHelp")}
          rules={{
            required: t("validateRealm"),
          }}
        />
        <TextControl
          name="config.serverPrincipal.0"
          label={t("serverPrincipal")}
          labelIcon={t("serverPrincipalHelp")}
          rules={{
            required: t("validateServerPrincipal"),
          }}
        />
        <TextControl
          name="config.keyTab.0"
          label={t("keyTab")}
          labelIcon={t("keyTabHelp")}
          rules={{
            required: t("validateKeyTab"),
          }}
        />
        <FormGroup
          label={t("debug")}
          labelIcon={
            <HelpItem helpText={t("debugHelp")} fieldLabelId="debug" />
          }
          fieldId="kc-debug"
          hasNoPaddingTop
        >
          <Controller
            name="config.debug"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-debug"}
                data-testid="debug"
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("debug")}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("allowPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t("allowPasswordAuthenticationHelp")}
              fieldLabelId="allowPasswordAuthentication"
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
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
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
                helpText={t("editModeKerberosHelp")}
                fieldLabelId="editMode"
              />
            }
            isRequired
            fieldId="kc-edit-mode"
          >
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
              helpText={t("updateFirstLoginHelp")}
              fieldLabelId="updateFirstLogin"
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
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value?.[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("updateFirstLogin")}
              />
            )}
          />
        </FormGroup>
      </FormAccess>
    </FormProvider>
  );
};
