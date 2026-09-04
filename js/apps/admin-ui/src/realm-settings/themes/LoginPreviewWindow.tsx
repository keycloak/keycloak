import { usePreviewLogo } from "./LogoContext";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { Environment } from "../../environment-types";
import { usePreviewBackground } from "./BackgroundContext";
import { LoginForm, LoginPage } from "@patternfly/react-core";
import { pf5VarsToPf5Css, pf5VarsToPf6Css } from "./pf5ToPf6Tokens";

type LoginPreviewWindowProps = {
  cssVars: Record<string, string>;
};

export const LoginPreviewWindow = ({ cssVars }: LoginPreviewWindowProps) => {
  const { environment } = useEnvironment<Environment>();
  const contextLogo = usePreviewLogo();
  const contextBackground = usePreviewBackground();

  // Resources
  const resourceUrlRoot = `/resources/${environment.resourceVersion}`;
  const loginResourceUrl = `${resourceUrlRoot}/login/keycloak.v3`;

  // Default login theme resources from local files
  const defaultBgImage = `${loginResourceUrl}/img/keycloak-bg-darken.svg`;
  const defaultLogo = `${loginResourceUrl}/img/keycloak-logo-text.svg`;

  // Use uploaded images or fall back to local defaults
  // Both logo and background come from context for immediate reactivity
  const logoUrl = contextLogo?.logo || defaultLogo;
  const bgUrl = contextBackground?.background || defaultBgImage;

  const logoWidth = cssVars["logoWidth"];
  const logoHeight = cssVars["logoHeight"];

  const themeCssVars = Object.fromEntries(
    Object.entries(cssVars).filter(
      ([key]) => key !== "logoWidth" && key !== "logoHeight",
    ),
  );

  const stylesThemeCssUrl = `${loginResourceUrl}/css/styles.css`;

  return (
    <>
      <link rel="stylesheet" href={stylesThemeCssUrl} />
      <style>{`
        .login-preview {
            ${pf5VarsToPf5Css(themeCssVars)}
            ${pf5VarsToPf6Css(themeCssVars)}

          /* Keycloak login theme variables - override with local/uploaded images */
          --keycloak-logo-url: url('${logoUrl}');
          --keycloak-bg-logo-url: url('${bgUrl}');
          ${logoHeight ? `--keycloak-logo-height: ${logoHeight};` : ""}
          ${logoWidth ? `--keycloak-logo-width: ${logoWidth};` : ""}
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
        .login-preview .pf-v6-c-login__main {
          anchor-name: --logo;
        }
        .login-preview .pf-v6-c-login__footer {
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
