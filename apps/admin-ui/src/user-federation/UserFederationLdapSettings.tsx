import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom-v5-compat";

import { useAlerts } from "../components/alert/Alerts";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { LdapMapperList } from "./ldap/mappers/LdapMapperList";
import {
  toUserFederationLdap,
  UserFederationLdapParams,
  UserFederationLdapTab,
} from "./routes/UserFederationLdap";
import { ExtendedHeader } from "./shared/ExtendedHeader";
import {
  LdapComponentRepresentation,
  serializeFormData,
  UserFederationLdapForm,
} from "./UserFederationLdapForm";

export default function UserFederationLdapSettings() {
  const { t } = useTranslation("user-federation");
  const form = useForm<LdapComponentRepresentation>({ mode: "onChange" });
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { id } = useParams<UserFederationLdapParams>();
  const { addAlert, addError } = useAlerts();
  const [component, setComponent] = useState<ComponentRepresentation>();
  const [refreshCount, setRefreshCount] = useState(0);

  const refresh = () => setRefreshCount((count) => count + 1);

  useFetch(
    () => adminClient.components.findOne({ id: id! }),
    (component) => {
      if (!component) {
        throw new Error(t("common:notFound"));
      }

      setComponent(component);
      setupForm(component);
    },
    [id, refreshCount]
  );

  const useTab = (tab: UserFederationLdapTab) =>
    useRoutableTab(toUserFederationLdap({ realm, id: id!, tab }));

  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");

  const setupForm = (component: ComponentRepresentation) => {
    form.reset(component);
    form.setValue(
      "config.periodicChangedUsersSync",
      component.config?.["changedSyncPeriod"]?.[0] !== "-1"
    );

    form.setValue(
      "config.periodicFullSync",
      component.config?.["fullSyncPeriod"]?.[0] !== "-1"
    );
  };

  const onSubmit = async (formData: LdapComponentRepresentation) => {
    try {
      await adminClient.components.update(
        { id: id! },
        serializeFormData(formData)
      );
      addAlert(t("saveSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("user-federation:saveError", error);
    }
  };

  if (!component) {
    return <KeycloakSpinner />;
  }

  return (
    <FormProvider {...form}>
      <ExtendedHeader
        provider="LDAP"
        noDivider
        editMode={component.config?.editMode}
        save={() => form.handleSubmit(onSubmit)()}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          defaultLocation={toUserFederationLdap({
            realm,
            id: id!,
            tab: "settings",
          })}
          isBox
        >
          <Tab
            id="settings"
            title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection variant="light">
              <UserFederationLdapForm id={id} onSubmit={onSubmit} />
            </PageSection>
          </Tab>
          <Tab
            id="mappers"
            title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
            data-testid="ldap-mappers-tab"
            {...mappersTab}
          >
            <LdapMapperList />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
