import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { Button, Form } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { ScrollForm } from "@keycloak/keycloak-ui-shared";

import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { toUserFederation } from "./routes/UserFederation";
import { SettingsCache } from "./shared/SettingsCache";

export type LdapComponentRepresentation = ComponentRepresentation & {
  config?: {
    periodicChangedUsersSync?: boolean;
    periodicFullSync?: boolean;
  };
};

export type UserFederationLdapFormProps = {
  id?: string;
  onSubmit: (formData: LdapComponentRepresentation) => void;
};

export const UserFederationLdapForm = ({
  id,
  onSubmit,
}: UserFederationLdapFormProps) => {
  const { t } = useTranslation();
  const form = useFormContext<LdapComponentRepresentation>();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();

  return (
    <>
      <ScrollForm
        label={t("jumpToSection")}
        sections={[
          {
            title: t("generalOptions"),
            panel: <LdapSettingsGeneral form={form} vendorEdit={!!id} />,
          },
          {
            title: t("connectionAndAuthenticationSettings"),
            panel: <LdapSettingsConnection form={form} id={id} />,
          },
          {
            title: t("ldapSearchingAndUpdatingSettings"),
            panel: <LdapSettingsSearching form={form} />,
          },
          {
            title: t("synchronizationSettings"),
            panel: <LdapSettingsSynchronization form={form} />,
          },
          {
            title: t("kerberosIntegration"),
            panel: <LdapSettingsKerberosIntegration form={form} />,
            isHidden: !isFeatureEnabled(Feature.Kerberos),
          },
          { title: t("cacheSettings"), panel: <SettingsCache form={form} /> },
          {
            title: t("advancedSettings"),
            panel: <LdapSettingsAdvanced form={form} id={id} />,
          },
        ]}
      />
      <Form onSubmit={form.handleSubmit(onSubmit)}>
        <FixedButtonsGroup
          name="ldap"
          isDisabled={!form.formState.isDirty}
          isSubmit
        >
          <Button
            variant="link"
            onClick={() => navigate(toUserFederation({ realm }))}
            data-testid="ldap-cancel"
          >
            {t("cancel")}
          </Button>
        </FixedButtonsGroup>
      </Form>
    </>
  );
};

export function serializeFormData(
  formData: LdapComponentRepresentation,
): LdapComponentRepresentation {
  const { config } = formData;

  if (config?.periodicChangedUsersSync !== undefined) {
    if (config.periodicChangedUsersSync === false) {
      config.changedSyncPeriod = ["-1"];
    }
    delete config.periodicChangedUsersSync;
  }

  if (config?.periodicFullSync !== undefined) {
    if (config.periodicFullSync === false) {
      config.fullSyncPeriod = ["-1"];
    }
    delete config.periodicFullSync;
  }

  return formData;
}
