import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { ThemesTabType, toThemesTab } from "../routes/ThemesTab";
import { QuickTheme } from "./QuickTheme";
import { ThemeSettingsTab } from "./ThemeSettings";

type ThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export default function ThemesTab({ realm, save }: ThemesTabProps) {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();

  const param = (tab: ThemesTabType) => ({
    realm: realmName,
    tab,
  });

  const settingsTab = useRoutableTab(toThemesTab(param("settings")));
  const quickThemeTab = useRoutableTab(toThemesTab(param("quickTheme")));

  if (!isFeatureEnabled(Feature.QuickTheme)) {
    return <ThemeSettingsTab realm={realm} save={save} />;
  }

  return (
    <RoutableTabs
      mountOnEnter
      unmountOnExit
      defaultLocation={toThemesTab({
        realm: realmName,
        tab: "settings",
      })}
    >
      <Tab
        id="themes-settings"
        title={<TabTitleText>{t("themes")} </TabTitleText>}
        data-testid="themes-settings-tab"
        {...settingsTab}
      >
        <ThemeSettingsTab realm={realm} save={save} />
      </Tab>
      <Tab
        id="quickTheme"
        title={<TabTitleText>{t("quickTheme")}</TabTitleText>}
        data-testid="quickTheme-tab"
        {...quickThemeTab}
      >
        <QuickTheme realm={realm} />
      </Tab>
    </RoutableTabs>
  );
}
