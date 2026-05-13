import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import JSZip from "jszip";
import { joinPath } from "../../utils/joinPath";
import { LogoContext } from "./LogoContext";
import { ThemeColors } from "./ThemeColors";

export type ThemeRealmRepresentation = RealmRepresentation & {
  themeName?: string;
  themeDescription?: string;
  fileName?: string;
  favicon?: File;
  logo?: File;
  bgimage?: File;
  logoWidth?: string;
  logoHeight?: string;
};

type QuickThemeProps = {
  realm: RealmRepresentation;
  theme?: "light" | "dark";
};

export const QuickTheme = ({ realm, theme }: QuickThemeProps) => {
  const { environment } = useEnvironment();

  const saveTheme = async (realm: ThemeRealmRepresentation) => {
    const zip = new JSZip();

    const styles = JSON.parse(realm.attributes?.style ?? "{}");
    const { favicon, logo, bgimage, fileName } = realm;

    const logoName =
      "img/logo" + logo?.name.substring(logo.name.lastIndexOf("."));
    const bgimageName =
      "img/bgimage" + bgimage?.name.substring(bgimage.name.lastIndexOf("."));

    const themeNameClean =
      (realm.themeName ?? "quick-theme")
        .trim()
        .toLowerCase()
        .normalize("NFKD") // decompose accents
        .replace(/[\u0300-\u036f]/g, "") // remove accents
        .replace(/[^a-z0-9._-]+/g, "-") // swap unsupported chars with '-'
        .replace(/-+/g, "-") // swap multiple '-' with single '-'
        .replace(/^[-._]+|[-._]+$/g, "") || "quick-theme"; // remove any '-' at string[0] or string[len-1]

    if (favicon) {
      zip.file(
        `theme/${themeNameClean}/common/resources/img/favicon.ico`,
        favicon,
      );
    }
    if (logo) {
      zip.file(`theme/${themeNameClean}/common/resources/${logoName}`, logo);
    }
    if (bgimage) {
      zip.file(
        `theme/${themeNameClean}/common/resources/${bgimageName}`,
        bgimage,
      );
    }

    zip.file(
      `theme/${themeNameClean}/admin/theme.properties`,
      `
parent=keycloak.v2
import=common/${themeNameClean}

${logo ? "logo=" + logoName : ""}
${favicon ? "favIcon=/img/favicon.ico" : ""}
styles=css/theme-styles.css
`,
    );

    zip.file(
      `theme/${themeNameClean}/admin/messages/messages_en.properties`,
      `theme.${themeNameClean}.admin.description=${realm.themeDescription ?? ""}`,
    );

    zip.file(
      `theme/${themeNameClean}/account/theme.properties`,
      `
parent=keycloak.v3
import=common/${themeNameClean}

${logo ? "logo=" + logoName : ""}
${favicon ? "favIcon=/img/favicon.ico" : ""}
styles=css/theme-styles.css
`,
    );

    zip.file(
      `theme/${themeNameClean}/account/messages/messages_en.properties`,
      `theme.${themeNameClean}.account.description=${realm.themeDescription ?? ""}`,
    );

    zip.file(
      `theme/${themeNameClean}/login/theme.properties`,
      `
parent=keycloak.v2
import=common/${themeNameClean}

styles=css/login.css css/theme-styles.css
`,
    );

    zip.file(
      `theme/${themeNameClean}/login/messages/messages_en.properties`,
      `theme.${themeNameClean}.login.description=${realm.themeDescription ?? ""}`,
    );

    zip.file(
      "META-INF/keycloak-themes.json",
      `{
  "themes": [{
      "name" : "${themeNameClean}",
      "types": [ "login", "account", "admin", "common" ]
  }]
}`,
    );

    zip.file(
      "theme-settings.json",
      JSON.stringify({
        ...styles,
        logo: logo
          ? `theme/${themeNameClean}/common/resources/${logoName}`
          : "",
        bgimage: bgimage
          ? `theme/${themeNameClean}/common/resources/${bgimageName}`
          : "",
        favicon: favicon
          ? `theme/${themeNameClean}/common/resources/img/favicon.ico`
          : "",
      }),
    );

    const toCss = (obj?: object) =>
      Object.entries(obj || {})
        .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
        .join("\n");

    const loginCss = (
      await fetch(joinPath(environment.resourceUrl, "/theme/login.css"))
    ).text();
    zip.file(
      `theme/${themeNameClean}/common/resources/css/styles.css`,
      loginCss,
    );

    zip.file(
      `theme/${themeNameClean}/common/resources/css/theme-styles.css`,
      `:root {
        ${bgimage ? `--keycloak-bg-logo-url: url('../${bgimageName}');` : ""}
        ${logo ? `--keycloak-logo-url: url('../${logoName}');` : ""}
        --keycloak-logo-height: ${realm.logoHeight};
        --keycloak-logo-width: ${realm.logoWidth};
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
      a.download = fileName || `${themeNameClean}.jar`;
      a.click();
      URL.revokeObjectURL(url);
    });
  };

  return (
    <LogoContext>
      <ThemeColors realm={realm} save={saveTheme} theme={theme} />
    </LogoContext>
  );
};
