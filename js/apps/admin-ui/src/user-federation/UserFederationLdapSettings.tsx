import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  LdapComponentRepresentation,
  UserFederationLdapForm,
  serializeFormData,
} from "./UserFederationLdapForm";
import { LdapMapperList } from "./ldap/mappers/LdapMapperList";
import {
  UserFederationLdapParams,
  toUserFederationLdap,
} from "./routes/UserFederationLdap";
import { toUserFederationLdapMapper } from "./routes/UserFederationLdapMapper";
import { ExtendedHeader } from "./shared/ExtendedHeader";

export default function UserFederationLdapSettings() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<LdapComponentRepresentation>({ mode: "onChange" });
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
        throw new Error(t("notFound"));
      }

      setComponent(component);
      setupForm(component);
    },
    [id, refreshCount],
  );

  const settingsTab = useRoutableTab(
    toUserFederationLdap({ realm, id: id!, tab: "settings" }),
  );
  const mappersTab = useRoutableTab(
    toUserFederationLdap({ realm, id: id!, tab: "mappers" }),
  );

  const setupForm = (component: ComponentRepresentation) => {
    form.reset({});
    form.reset(component);
    form.setValue(
      "config.periodicChangedUsersSync",
      component.config?.["changedSyncPeriod"]?.[0] !== "-1",
    );

    form.setValue(
      "config.periodicFullSync",
      component.config?.["fullSyncPeriod"]?.[0] !== "-1",
    );
  };

  const onSubmit = async (formData: LdapComponentRepresentation) => {
    try {
      await adminClient.components.update(
        { id: id! },
        serializeFormData(formData),
      );
      addAlert(t("userProviderSaveSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("userProviderSaveError", error);
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
      <PageSection variant="light" className="pf-v5-u-p-0">
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
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection variant="light">
              <UserFederationLdapForm id={id} onSubmit={onSubmit} />
            </PageSection>
          </Tab>
          <Tab
            id="mappers"
            title={<TabTitleText>{t("mappers")}</TabTitleText>}
            data-testid="ldap-mappers-tab"
            {...mappersTab}
          >
            <LdapMapperList
              toCreate={toUserFederationLdapMapper({
                realm,
                id: id!,
                mapperId: "new",
              })}
              toDetail={(mapperId) =>
                toUserFederationLdapMapper({
                  realm,
                  id: id!,
                  mapperId,
                })
              }
            />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
