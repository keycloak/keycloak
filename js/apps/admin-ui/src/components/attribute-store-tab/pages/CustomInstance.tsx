import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";

import { useRealm } from "../../../context/realm-context/RealmContext";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { toUsers } from "../../../user/routes/Users";

import { ViewHeader } from "../../view-header/ViewHeader";
import { RoutableTabs, useRoutableTab } from "../../routable-tabs/RoutableTabs";
import {
  CustomAttributeStoreInstanceRouteParams,
  CustomAttributeStoreInstanceTab,
  toCustomAttributeStoreInstance,
} from "../routes/CustomInstance";

import { CustomInstanceSettingsTab } from "./CustomInstanceSettingsTab";

export default function CustomProviderSettings() {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const navigate = useNavigate();

  const { id, providerId } =
    useParams<CustomAttributeStoreInstanceRouteParams>();

  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });

  const useTab = (tab: CustomAttributeStoreInstanceTab) =>
    useRoutableTab(
      toCustomAttributeStoreInstance({
        realm: realmName,
        providerId: providerId!,
        id: id!,
        tab,
      }),
    );

  const settingsTab = useTab("settings");

  const provider = (
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.attributes.AttributeStoreProvider"
    ] || []
  ).find((p) =>
    providerId
      ? p.id === providerId
      : navigate(toUsers({ realm: realmName, tab: "attributeStore" })),
  );

  return (
    <FormProvider {...form}>
      <ViewHeader
        divider={false}
        titleKey={provider?.metadata.displayName || provider?.id}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          defaultLocation={toCustomAttributeStoreInstance({
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
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
