import { usePreviewLogo } from "./LogoContext";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { Environment } from "../../environment-types";
import { joinPath } from "../../utils/joinPath";
import { usePreviewBackground } from "./BackgroundContext";
import { pf5VarsToPf6Css } from "./pf5ToPf6Tokens";

type LoginPreviewWindowProps = {
  cssVars: Record<string, string>;
};

/**
 * Renders a static PatternFly 6 login preview that mirrors the keycloak.v3
 * FreeMarker markup. Admin UI itself remains on PatternFly React 5, so this
 * loads the theme CSS instead of using @patternfly/react-core LoginPage.
 */
export const LoginPreviewWindow = ({ cssVars }: LoginPreviewWindowProps) => {
  const { environment } = useEnvironment<Environment>();
  const contextLogo = usePreviewLogo();
  const contextBackground = usePreviewBackground();

  const resourceUrlRoot = joinPath(
    environment.adminBaseUrl,
    "resources",
    environment.resourceVersion,
  );
  const loginResourceUrl = `${resourceUrlRoot}/login/keycloak.v3`;
  const commonResourceUrl = `${resourceUrlRoot}/common/keycloak`;

  const defaultBgImage = `${loginResourceUrl}/img/keycloak-bg-darken.svg`;
  const defaultLogo = `${loginResourceUrl}/img/keycloak-logo-text.svg`;

  const logoUrl = contextLogo?.logo || defaultLogo;
  const bgUrl = contextBackground?.background || defaultBgImage;

  // Logo dimensions are theme CSS custom properties, not PatternFly tokens.
  const logoWidth = cssVars["logoWidth"];
  const logoHeight = cssVars["logoHeight"];
  const themeCssVars = Object.fromEntries(
    Object.entries(cssVars).filter(
      ([key]) => key !== "logoWidth" && key !== "logoHeight",
    ),
  );

  return (
    <>
      <link
        rel="stylesheet"
        href={`${commonResourceUrl}/vendor/patternfly-v6/patternfly.min.css`}
      />
      <link
        rel="stylesheet"
        href={`${commonResourceUrl}/vendor/patternfly-v6/patternfly-addons.css`}
      />
      <link rel="stylesheet" href={`${loginResourceUrl}/css/styles.css`} />
      <style>{`
        .login-preview {
          ${pf5VarsToPf6Css(themeCssVars)}

          --keycloak-logo-url: url('${logoUrl}');
          --keycloak-bg-logo-url: url('${bgUrl}');
          ${logoHeight ? `--keycloak-logo-height: ${logoHeight};` : ""}
          ${logoWidth ? `--keycloak-logo-width: ${logoWidth};` : ""}

          /* Theme CSS targets "login-pf body"; preview is not a document body. */
          background: var(--keycloak-bg-logo-url) no-repeat center center;
          background-size: cover;
          min-height: 40rem;
          padding: 2rem 1rem;
        }
      `}</style>
      <div className="login-preview login-pf" data-testid="login-page-preview">
        <div className="pf-v6-c-login">
          <div className="pf-v6-c-login__container">
            <header id="kc-header" className="pf-v6-c-login__header">
              <div id="kc-header-wrapper" className="pf-v6-c-brand">
                <div className="kc-logo-text">
                  <span>Keycloak</span>
                </div>
              </div>
            </header>
            <main className="pf-v6-c-login__main">
              <div className="pf-v6-c-login__main-header">
                <h1 className="pf-v6-c-title pf-m-3xl" id="kc-page-title">
                  Sign in to your account
                </h1>
              </div>
              <div className="pf-v6-c-login__main-body">
                <div id="kc-form">
                  <div id="kc-form-wrapper">
                    <form
                      id="kc-form-login"
                      className="pf-v6-c-form pf-v6-u-w-100"
                      onSubmit={(event) => event.preventDefault()}
                    >
                      <div className="pf-v6-c-form__group">
                        <div className="pf-v6-c-form__group-label pf-v6-u-pb-xs">
                          <label
                            htmlFor="username"
                            className="pf-v6-c-form__label"
                          >
                            <span className="pf-v6-c-form__label-text">
                              Username or email
                            </span>
                          </label>
                        </div>
                        <span className="pf-v6-c-form-control">
                          <input
                            id="username"
                            name="username"
                            type="text"
                            autoComplete="username"
                            readOnly
                          />
                        </span>
                      </div>
                      <div className="pf-v6-c-form__group">
                        <div className="pf-v6-c-form__group-label pf-v6-u-pb-xs">
                          <label
                            htmlFor="password"
                            className="pf-v6-c-form__label"
                          >
                            <span className="pf-v6-c-form__label-text">
                              Password
                            </span>
                          </label>
                        </div>
                        <div className="pf-v6-c-input-group">
                          <div className="pf-v6-c-input-group__item pf-m-fill">
                            <span className="pf-v6-c-form-control">
                              <input
                                id="password"
                                name="password"
                                type="password"
                                autoComplete="current-password"
                                readOnly
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <div className="pf-v6-c-form__group">
                        <div className="pf-v6-c-form__actions pf-v6-u-pt-xs pf-v6-u-flex-wrap">
                          <button
                            className="pf-v6-c-button pf-m-primary pf-m-block"
                            id="kc-login"
                            name="login"
                            type="submit"
                          >
                            Sign In
                          </button>
                        </div>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            </main>
          </div>
        </div>
      </div>
    </>
  );
};
