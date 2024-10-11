import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { ThemesTabType, toThemesTab } from "../routes/ThemesTab";
import { ThemeColors } from "./ThemeColors";
import { ThemeSettingsTab } from "./ThemeSettings";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";

type ThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export default function ThemesTab({ realm, save }: ThemesTabProps) {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();

  const useThemesTab = (tab: ThemesTabType) =>
    useRoutableTab(
      toThemesTab({
        realm: realmName,
        tab,
      }),
    );

  const settingsTab = useThemesTab("settings");
  const lightColorsTab = useThemesTab("lightColors");
  const darkColorsTab = useThemesTab("darkColors");

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
        id="lightColors"
        title={<TabTitleText>{t("themeColorsLight")}</TabTitleText>}
        data-testid="lightColors-tab"
        {...lightColorsTab}
      >
        <ThemeColors realm={realm} save={save} theme="light" />
      </Tab>
      <Tab
        id="darkColors"
        title={<TabTitleText>{t("themeColorsDark")}</TabTitleText>}
        data-testid="darkColors-tab"
        {...darkColorsTab}
      >
        <ThemeColors realm={realm} save={save} theme="dark" />
      </Tab>
    </RoutableTabs>
  );
}
