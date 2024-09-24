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

type ThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export default function ThemesTab({ realm, save }: ThemesTabProps) {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();

  const useThemesTab = (tab: ThemesTabType) =>
    useRoutableTab(
      toThemesTab({
        realm: realmName,
        tab,
      }),
    );

  const settingsTab = useThemesTab("settings");
  const colorsTab = useThemesTab("colors");

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
        id="colors"
        title={<TabTitleText>{t("themeColors")}</TabTitleText>}
        data-testid="colors-tab"
        {...colorsTab}
      >
        <ThemeColors realm={realm} save={save} />
      </Tab>
    </RoutableTabs>
  );
}
