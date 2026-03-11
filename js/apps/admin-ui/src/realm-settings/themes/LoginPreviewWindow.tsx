import { usePreviewLogo } from "./LogoContext";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { Environment } from "../../environment-types";
import { usePreviewBackground } from "./BackgroundContext";
import { LoginForm, LoginPage } from "@patternfly/react-core";
import { borderRadiusToCss } from "./BorderRadiusControl";

type LoginPreviewWindowProps = {
  cssVars: Record<string, string>;
};

export const LoginPreviewWindow = ({ cssVars }: LoginPreviewWindowProps) => {
  const { environment } = useEnvironment<Environment>();
  const contextLogo = usePreviewLogo();
  const contextBackground = usePreviewBackground();

  // Resources
  const resourceUrlRoot = `/resources/${environment.resourceVersion}`;
  const loginResourceUrl = `${resourceUrlRoot}/login/keycloak.v2`;

  // Default login theme resources from local files
  const defaultBgImage = `${loginResourceUrl}/img/keycloak-bg-darken.svg`;
  const defaultLogo = `${loginResourceUrl}/img/keycloak-logo-text.svg`;

  // Use uploaded images or fall back to local defaults
  // Both logo and background come from context for immediate reactivity
  const logoUrl = contextLogo?.logo || defaultLogo;
  const bgUrl = contextBackground?.background || defaultBgImage;

  const logoWidth = cssVars["logoWidth"];
  const logoHeight = cssVars["logoHeight"];

  const stylesThemeCssUrl = `${loginResourceUrl}/css/styles.css`;

  return (
    <>
      <link rel="stylesheet" href={stylesThemeCssUrl} />
      <style>{`
        .login-preview {
            ${Object.entries(cssVars)
              .filter(
                ([key]) =>
                  !["logoWidth", "logoHeight", "borderRadius"].includes(key),
              )
              .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
              .join("\n")}

          /* Keycloak login theme variables - override with local/uploaded images */
          --keycloak-logo-url: url('${logoUrl}');
          --keycloak-bg-logo-url: url('${bgUrl}');
          ${logoHeight ? `--keycloak-logo-height: ${logoHeight};` : ""}
          ${logoWidth ? `--keycloak-logo-width: ${logoWidth};` : ""}
          ${borderRadiusToCss(cssVars)}
        }

        /* Apply background to #keycloak-bg */
        .login-preview {
          background: var(--keycloak-bg-logo-url);
          background-size: cover;
        }

        .login-preview .kc-logo-text {
          position: absolute;
          position-anchor: --logo;
          position-area: start center;
          margin-bottom: 3rem;
        }
        .login-preview .pf-v5-c-login__main {
          anchor-name: --logo;
        }
        .login-preview .pf-v5-c-login__footer {
          display: none;
        }
      `}</style>
      <div className="login-preview login-pf">
        <div className="kc-logo-text">
          <span>Keycloak</span>
        </div>
        <LoginPage loginTitle="Sign in to your account">
          <LoginForm
            usernameLabel="Username or email"
            loginButtonLabel="Sign In"
          />
        </LoginPage>
      </div>
    </>
  );
};
