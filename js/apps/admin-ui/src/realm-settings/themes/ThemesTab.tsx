import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Tab, TabTitleText } from "@patternfly/react-core";
import JSZip from "jszip";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { ThemesTabType, toThemesTab } from "../routes/ThemesTab";
import { ThemeColors } from "./ThemeColors";
import { ThemeSettingsTab } from "./ThemeSettings";
import { joinPath } from "../../utils/joinPath";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";

type ThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export type ThemeRealmRepresentation = RealmRepresentation & {
  logo?: File;
  bgimage?: File;
};

export default function ThemesTab({ realm, save }: ThemesTabProps) {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const { environment } = useEnvironment();
  const isFeatureEnabled = useIsFeatureEnabled();

  const saveTheme = async (realm: ThemeRealmRepresentation) => {
    const zip = new JSZip();

    const styles = JSON.parse(realm.attributes?.style ?? "{}");

    const { logo, bgimage, ...rest } = realm;

    const logoName =
      "img/logo" + logo?.name?.substring(logo?.name?.lastIndexOf("."));
    const bgimageName =
      "img/bgimage" + bgimage?.name?.substring(bgimage?.name?.lastIndexOf("."));

    if (logo) {
      zip.file(`common/resources/${logoName}`, logo);
    }
    if (bgimage) {
      zip.file(`common/resources/${bgimageName}`, bgimage);
    }

    zip.file(
      "admin/theme.properties",
      `
parent=keycloak.v2
import=common/quick-theme

styles=css/theme-styles.css
`,
    );

    zip.file(
      "account/theme.properties",
      `
parent=keycloak.v3
import=common/quick-theme

styles=css/theme-styles.css
`,
    );

    zip.file(
      "login/theme.properties",
      `
parent=keycloak.v2
import=common/quick-theme

styles=css/login.css css/theme-styles.css
`,
    );

    const toCss = (obj?: object) =>
      Object.entries(obj || {})
        .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
        .join("\n");

    const logoCss = (
      await fetch(joinPath(environment.resourceUrl, "/theme/login.css"))
    ).text();
    zip.file("common/resources/css/login.css", logoCss);

    zip.file(
      "common/resources/css/theme-styles.css",
      `:root {
        --keycloak-bg-logo-url: url('../${bgimageName}');
        --keycloak-logo-url: url('../${logoName}');
        --keycloak-logo-height: 63px;
        --keycloak-logo-width: 300px;
        ${toCss(styles.light)}
      }
      .pf-v5-theme-dark {
        ${toCss(styles.dark)}
      }
      `,
    );
    save({
      ...rest,
      attributes: {
        ...rest.attributes,
        style: JSON.stringify({
          ...styles,
          logo: logoName,
          bgimage: bgimageName,
        }),
      },
    });
    zip.generateAsync({ type: "blob" }).then((content) => {
      const url = URL.createObjectURL(content);
      const a = document.createElement("a");
      a.href = url;
      a.download = "quick-theme.jar";
      a.click();
      URL.revokeObjectURL(url);
    });
  };

  const param = (tab: ThemesTabType) => ({
    realm: realmName,
    tab,
  });

  const settingsTab = useRoutableTab(toThemesTab(param("settings")));
  const lightColorsTab = useRoutableTab(toThemesTab(param("lightColors")));
  const darkColorsTab = useRoutableTab(toThemesTab(param("darkColors")));

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
        <ThemeColors realm={realm} save={saveTheme} theme="light" />
      </Tab>
      <Tab
        id="darkColors"
        title={<TabTitleText>{t("themeColorsDark")}</TabTitleText>}
        data-testid="darkColors-tab"
        {...darkColorsTab}
      >
        <ThemeColors realm={realm} save={saveTheme} theme="dark" />
      </Tab>
    </RoutableTabs>
  );
}
