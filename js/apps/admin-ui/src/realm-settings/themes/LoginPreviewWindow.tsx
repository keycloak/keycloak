import { loginThemeProperties as properties } from "./LoginThemeProperties";
import { usePreviewLogo } from "./LogoContext";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { Environment } from "../../environment";
import { usePreviewBackground } from "./BackgroundContext";

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

  // CSS files
  const pf5CssRootPath = `${resourceUrlRoot}/common/keycloak/vendor/patternfly-v5`;
  const pf5Css = `${pf5CssRootPath}/patternfly.min.css`;
  const pf5AddonsCss = `${pf5CssRootPath}/patternfly-addons.css`;

  const stylesThemeCssUrl = `${loginResourceUrl}/css/styles.css`;

  return (
    <>
      <link rel="stylesheet" href={pf5Css} />
      <link rel="stylesheet" href={pf5AddonsCss} />
      <link rel="stylesheet" href={stylesThemeCssUrl} />

      <style>{`
        .login-preview {
            ${Object.entries(cssVars)
              .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
              .join("\n")}

          /* Keycloak login theme variables - override with local/uploaded images */
          --keycloak-logo-url: url('${logoUrl}');
          --keycloak-bg-logo-url: url('${bgUrl}');
          ${logoHeight ? `--keycloak-logo-height: ${logoHeight};` : ""}
          ${logoWidth ? `--keycloak-logo-width: ${logoWidth};` : ""}
        }

        /* Apply background to #keycloak-bg */
        .login-preview.${properties.kcHtmlClass} {
          background: var(--keycloak-bg-logo-url) no-repeat center center;
          background-size: cover;
        }

        /* Ensure login container is properly sized */
        .login-preview .${properties.kcLogin} {
            min-height:70vh;
        }

        /* Force single column layout */
        .login-preview .${properties.kcLoginContainer} {
          grid-template-columns: 34rem !important;
          grid-template-areas: "header"
                               "main" !important;
        }
      `}</style>

      <div className="login-preview-wrapper">
        <div className={`login-preview ${properties.kcHtmlClass}`}>
          <div id="keycloak-bg" data-page-id="login-preview">
            <div className={properties.kcLogin}>
              <div className={properties.kcLoginContainer}>
                <header id="kc-header" className="pf-v5-c-login__header">
                  <div id="kc-header-wrapper" className="pf-v5-c-brand">
                    <div className="kc-logo-text">
                      <span>Keycloak</span>
                    </div>
                  </div>
                </header>
                <main className={properties.kcLoginMain}>
                  <div className={properties.kcLoginMainHeader}>
                    <h1
                      className={properties.kcLoginMainTitle}
                      id="kc-page-title"
                    >
                      Sign in to your account
                    </h1>
                  </div>
                  <div className={properties.kcLoginMainBody}>
                    <div id="kc-form">
                      <div id="kc-form-wrapper">
                        <form
                          id="kc-form-login"
                          className={properties.kcFormClass}
                          onSubmit={(e) => e.preventDefault()}
                          noValidate
                        >
                          {/* Username field */}
                          <div className={properties.kcFormGroupClass}>
                            <div className={properties.kcFormGroupLabelClass}>
                              <label
                                htmlFor="username"
                                className={properties.kcFormLabelClass}
                              >
                                <span
                                  className={properties.kcFormLabelTextClass}
                                >
                                  Username or email
                                </span>
                              </label>
                            </div>
                            <span className={properties.kcInputClass}>
                              <input
                                id="username"
                                name="username"
                                value=""
                                type="text"
                                autoComplete="username"
                                readOnly
                              />
                            </span>
                            <div id="input-error-container-username"></div>
                          </div>

                          {/* Password field */}
                          <div className={properties.kcFormGroupClass}>
                            <div className={properties.kcFormGroupLabelClass}>
                              <label
                                htmlFor="password"
                                className={properties.kcFormLabelClass}
                              >
                                <span
                                  className={properties.kcFormLabelTextClass}
                                >
                                  Password
                                </span>
                              </label>
                            </div>
                            <div className={properties.kcInputGroup}>
                              <div
                                className={`${properties.kcInputGroupItemClass} ${properties.kcFill}`}
                              >
                                <span className={properties.kcInputClass}>
                                  <input
                                    id="password"
                                    name="password"
                                    value=""
                                    type={"password"}
                                    autoComplete="current-password"
                                    readOnly
                                  />
                                </span>
                              </div>
                              <div className={properties.kcInputGroupItemClass}>
                                <button
                                  className={
                                    properties.kcFormPasswordVisibilityButtonClass
                                  }
                                  type="button"
                                  aria-label={"Show password"}
                                >
                                  <i
                                    className={
                                      properties.kcFormPasswordVisibilityIconShow
                                    }
                                    aria-hidden="true"
                                  ></i>
                                </button>
                              </div>
                            </div>
                            <div
                              className={properties.kcFormHelperTextClass}
                              aria-live="polite"
                            >
                              <div
                                className={properties.kcInputHelperTextClass}
                              ></div>
                            </div>
                            <div id="input-error-container-password"></div>
                          </div>

                          {/* Submit button */}
                          <div className={properties.kcFormGroupClass}>
                            <div className={properties.kcFormActionGroupClass}>
                              <button
                                className={`${properties.kcButtonPrimaryClass} ${properties.kcButtonBlockClass}`}
                                name="login"
                                id="kc-login"
                                type="submit"
                              >
                                Sign In
                              </button>
                            </div>
                          </div>
                        </form>
                      </div>
                    </div>

                    <div className={properties.kcLoginMainFooter}>
                      {/* Social providers or additional info would go here */}
                    </div>
                  </div>

                  <div className={properties.kcLoginMainFooter}></div>
                </main>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
