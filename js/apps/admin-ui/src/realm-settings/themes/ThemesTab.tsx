import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { Tab, TabTitleText } from "@patternfly/react-core";
import JSZip from "jszip";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { joinPath } from "../../utils/joinPath";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { ThemesTabType, toThemesTab } from "../routes/ThemesTab";
import { LogoContext } from "./LogoContext";
import { ThemeColors } from "./ThemeColors";
import { ThemeSettingsTab } from "./ThemeSettings";

type ThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export type ThemeRealmRepresentation = RealmRepresentation & {
  fileName?: string;
  favicon?: File;
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
    const { favicon, logo, bgimage, fileName } = realm;

    const logoName =
      "img/logo" + logo?.name?.substring(logo?.name?.lastIndexOf("."));
    const bgimageName =
      "img/bgimage" + bgimage?.name?.substring(bgimage?.name?.lastIndexOf("."));

    if (favicon) {
      zip.file(`theme/quick-theme/common/resources/img/favicon.ico`, favicon);
    }
    if (logo) {
      zip.file(`theme/quick-theme/common/resources/${logoName}`, logo);
    }
    if (bgimage) {
      zip.file(`theme/quick-theme/common/resources/${bgimageName}`, bgimage);
    }

    zip.file(
      "theme/quick-theme/admin/theme.properties",
      `
parent=keycloak.v2
import=common/quick-theme

${logo ? "logo=" + logoName : ""}
styles=css/theme-styles.css
`,
    );

    zip.file(
      "theme/quick-theme/account/theme.properties",
      `
parent=keycloak.v3
import=common/quick-theme

${logo ? "logo=" + logoName : ""}
styles=css/theme-styles.css
`,
    );

    zip.file(
      "theme/quick-theme/login/theme.properties",
      `
parent=keycloak.v2
import=common/quick-theme

styles=css/login.css css/theme-styles.css
`,
    );

    zip.file(
      "META-INF/keycloak-themes.json",
      `{
  "themes": [{
      "name" : "quick-theme",
      "types": [ "login", "account", "admin", "common" ]
  }]
}`,
    );

    zip.file(
      "theme-settings.json",
      JSON.stringify({
        ...styles,
        logo: logo ? `theme/quick-theme/common/resources/${logoName}` : "",
        bgimage: bgimage
          ? `theme/quick-theme/common/resources/${bgimageName}`
          : "",
        favicon: favicon
          ? "theme/quick-theme/common/resources/img/favicon.ico"
          : "",
      }),
    );

    const toCss = (obj?: object) =>
      Object.entries(obj || {})
        .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
        .join("\n");

    const logoCss = (
      await fetch(joinPath(environment.resourceUrl, "/theme/login.css"))
    ).text();
    zip.file("theme/quick-theme/common/resources/css/login.css", logoCss);

    zip.file(
      "theme/quick-theme/common/resources/css/theme-styles.css",
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
    await zip.generateAsync({ type: "blob" }).then((content) => {
      const url = URL.createObjectURL(content);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName || "quick-theme.jar";
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
        <LogoContext>
          <ThemeColors realm={realm} save={saveTheme} theme="light" />
        </LogoContext>
      </Tab>
      <Tab
        id="darkColors"
        title={<TabTitleText>{t("themeColorsDark")}</TabTitleText>}
        data-testid="darkColors-tab"
        {...darkColorsTab}
      >
        <LogoContext>
          <ThemeColors realm={realm} save={saveTheme} theme="dark" />
        </LogoContext>
      </Tab>
    </RoutableTabs>
  );
}
