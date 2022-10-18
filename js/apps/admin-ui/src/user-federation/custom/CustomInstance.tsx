import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import {
  CustomUserFederationRouteParams,
  CustomUserFederationInstanceTab,
  toCustomUserFederation,
} from "../routes/CustomUserFederation";
import { toUserFederationMapper } from "../routes/CustomInstanceMapper";

import { CustomInstanceSettingsTab } from "./CustomInstanceSettingsTab";
import { CustomInstanceMappersTab } from "./CustomInstanceMappersTab";

export default function CustomProviderSettings() {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();

  const { id, providerId } = useParams<CustomUserFederationRouteParams>();

  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });

  const useTab = (tab: CustomUserFederationInstanceTab) =>
    useRoutableTab(
      toCustomUserFederation({
        realm: realmName,
        providerId: providerId!,
        id: id!,
        tab,
      }),
    );

  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");

  const provider = (
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.UserStorageProvider"
    ] || []
  ).find((p: ComponentTypeRepresentation) => p.id === providerId);

  return (
    <FormProvider {...form}>
      <ViewHeader
        divider={false}
        titleKey={provider?.metadata.displayName || provider?.id}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          defaultLocation={toCustomUserFederation({
            realm: realmName,
            providerId: providerId!,
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
              <CustomInstanceSettingsTab />
            </PageSection>
          </Tab>
          {!!id && provider?.metadata.mapperType && (
            <Tab
              id="mappers"
              title={<TabTitleText>{t("mappers")}</TabTitleText>}
              {...mappersTab}
            >
              <PageSection variant="light">
                <CustomInstanceMappersTab
                  toPage={(mapperId?: string) =>
                    toUserFederationMapper({
                      realm: realmName,
                      providerId: providerId!,
                      parentId: id,
                      id: mapperId,
                    })
                  }
                />
              </PageSection>
            </Tab>
          )}
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
