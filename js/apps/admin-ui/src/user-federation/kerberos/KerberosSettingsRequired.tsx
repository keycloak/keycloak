import {
  HelpItem,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, Switch } from "@patternfly/react-core";
import { isEqual } from "lodash-es";
import { useEffect } from "react";
import {
  Controller,
  FormProvider,
  UseFormReturn,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
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
  const { t } = useTranslation();
  const { realm, realmRepresentation } = useRealm();

  const allowPassAuth = useWatch({
    control: form.control,
    name: "config.allowPasswordAuthentication",
  });

  useEffect(() => form.setValue("parentId", realmRepresentation?.id), []);

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
          <SelectControl
            name="config.editMode[0]"
            label={t("editMode")}
            labelIcon={t("editModeKerberosHelp")}
            controller={{
              rules: { required: t("required") },
              defaultValue: "READ_ONLY",
            }}
            options={["READ_ONLY", "UNSYNCED"]}
          />
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
