<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Welcome to ${productName}</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="shortcut icon" href="${resourcesPath}/img/favicon.ico">
    <#if properties.stylesCommon?has_content>
      <#list properties.stylesCommon?split(' ') as style>
        <link href="${resourcesCommonPath}/${style}" rel="stylesheet">
      </#list>
    </#if>
    <#if properties.styles?has_content>
      <#list properties.styles?split(' ') as style>
        <link href="${resourcesPath}/${style}" rel="stylesheet">
      </#list>
    </#if>
  </head>
  <body>
    <div class="pf-v5-c-page">
      <main class="pf-v5-c-page__main">
        <section class="pf-v5-c-page__main-section pf-m-limit-width pf-m-align-center">
          <div class="pf-v5-c-page__main-body">
            <div class="pf-v5-c-content">
              <h1 class="pf-v5-c-title pf-m-2xl pf-v5-u-mb-lg">Welcome to <strong>${productName}</strong></h1>
            </div>
            <div class="pf-v5-l-grid pf-m-gutter pf-m-all-4-col-on-sm">
              <#if adminConsoleEnabled>
                <div class="pf-v5-l-grid__item">
                  <div class="pf-v5-c-card pf-m-full-height">
                    <div class="pf-v5-c-card__title">
                      <h2 class="pf-v5-c-card__title-text">Administration Console</h2>
                    </div>
                    <div class="pf-v5-c-card__body">
                      <#if bootstrap>
                        <#if localUser>
                          <div class="pf-v5-c-content">
                            <p class="pf-v5-u-mb-sm">Please create an initial admin user to get started.</p>
                          </div>
                          <form id="create-user" class="pf-v5-c-form" method="post" novalidate>
                            <div class="pf-v5-c-form__group">
                              <div class="pf-v5-c-form__group-label">
                                <label class="pf-v5-c-form__label" for="username">
                                  <span class="pf-v5-c-form__label-text">Username</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                                </label>
                              </div>
                              <div class="pf-v5-c-form__group-control">
                                <span class="pf-v5-c-form-control pf-m-required">
                                  <input id="username" type="text" name="username" autocomplete="username" required>
                                </span>
                              </div>
                            </div>
                            <div class="pf-v5-c-form__group">
                              <div class="pf-v5-c-form__group-label">
                                <label class="pf-v5-c-form__label" for="password">
                                  <span class="pf-v5-c-form__label-text">Password</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                                </label>
                              </div>
                              <div class="pf-v5-c-form__group-control">
                                <span class="pf-v5-c-form-control pf-m-required">
                                  <input id="password" type="password" name="password" autocomplete="new-password" required>
                                </span>
                              </div>
                            </div>
                            <div class="pf-v5-c-form__group">
                              <div class="pf-v5-c-form__group-label">
                                <label class="pf-v5-c-form__label" for="password-confirmation">
                                  <span class="pf-v5-c-form__label-text">Password confirmation</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                                </label>
                              </div>
                              <div class="pf-v5-c-form__group-control">
                                <span class="pf-v5-c-form-control pf-m-required">
                                  <input id="password-confirmation" type="password" name="passwordConfirmation" autocomplete="new-password" required>
                                </span>
                              </div>
                            </div>
                            <input name="stateChecker" type="hidden" value="${stateChecker}">
                            <#if errorMessage?has_content>
                              <div class="pf-v5-c-alert pf-m-danger pf-m-plain pf-m-inline pf-v5-u-mb-sm">
                                <div class="pf-v5-c-alert__icon">
                                  <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
                                </div>
                                <p class="pf-v5-c-alert__title">
                                  ${errorMessage}
                                </p>
                              </div>
                            </#if>
                            <div class="pf-v5-c-form__group pf-m-action">
                              <div class="pf-v5-c-form__actions">
                                <button class="pf-v5-c-button pf-m-primary" type="submit" form="create-user">Create user</button>
                              </div>
                            </div>
                          </form>
                        <#else>
                          <div class="pf-v5-c-content">
                            <p>
                              You will need local access to create the initial admin user.<br><br>
                              To create one open <a href="${localAdminUrl}">${localAdminUrl}</a>, or set the environment variables <code>KEYCLOAK_ADMIN</code> and <code>KEYCLOAK_ADMIN_PASSWORD</code> when starting the server.
                            </p>
                          </div>
                        </#if>
                      <#else>
                        <div class="pf-v5-c-content">
                          <p>Centrally manage all aspects of the ${productName} server.</p>
                        </div>
                        <#if successMessage?has_content>
                          <div class="pf-v5-c-alert pf-m-success pf-m-inline pf-v5-u-mt-lg">
                            <div class="pf-v5-c-alert__icon">
                              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
                            </div>
                            <p class="pf-v5-c-alert__title">
                              ${successMessage}
                            </p>
                          </div>
                        </#if>
                        <div class="pf-v5-c-card__footer">
                          <a class="pf-v5-c-button pf-m-link pf-m-inline" href="${adminUrl}">
                            Visit the administration console
                            <span class="pf-v5-c-button__icon pf-m-end">
                              <i class="fas fa-arrow-right" aria-hidden="true"></i>
                            </span>
                          </a>
                        </div>
                      </#if>
                    </div>
                  </div>
                </div>
              </#if>
              <div class="pf-v5-l-grid__item">
                <div class="pf-v5-c-card pf-m-full-height">
                  <div class="pf-v5-c-card__title">
                    <h2 class="pf-v5-c-card__title-text">Documentation</h2>
                  </div>
                  <div class="pf-v5-c-card__body">User Guide, Admin REST API and Javadocs.</div>
                  <div class="pf-v5-c-card__footer">
                    <a class="pf-v5-c-button pf-m-link pf-m-inline" href="${properties.documentationUrl}">
                      Read the documentation
                      <span class="pf-v5-c-button__icon pf-m-end">
                        <i class="fas fa-arrow-right" aria-hidden="true"></i>
                      </span>
                    </a>
                  </div>
                </div>
              </div>
              <#if properties.displayCommunityLinks = "true">
                <div class="pf-v5-l-grid__item">
                  <div class="pf-v5-l-grid pf-m-gutter">
                    <div class="pf-v5-l-grid__item pf-m-12-col">
                      <div class="pf-v5-c-card">
                        <div class="pf-v5-c-card__title">
                          <h2 class="pf-v5-c-card__title-text">${productName} Project</h2>
                        </div>
                        <div class="pf-v5-c-card__body">The home page of the ${productName} project.</div>
                        <div class="pf-v5-c-card__footer">
                          <a class="pf-v5-c-button pf-m-link pf-m-inline" href="https://www.keycloak.org/">
                            Visit the ${productName} project
                            <span class="pf-v5-c-button__icon pf-m-end">
                              <i class="fas fa-arrow-right" aria-hidden="true"></i>
                            </span>
                          </a>
                        </div>
                      </div>
                    </div>
                    <div class="pf-v5-l-grid__item pf-m-12-col">
                      <div class="pf-v5-c-card">
                        <div class="pf-v5-c-card__title">
                          <h2 class="pf-v5-c-card__title-text">Mailing List</h2>
                        </div>
                        <div class="pf-v5-c-card__body">Discussions about ${productName}.</div>
                        <div class="pf-v5-c-card__footer">
                          <a class="pf-v5-c-button pf-m-link pf-m-inline" href="https://groups.google.com/g/keycloak-user">
                            Start a discussion
                            <span class="pf-v5-c-button__icon pf-m-end">
                              <i class="fas fa-arrow-right" aria-hidden="true"></i>
                            </span>
                          </a>
                        </div>
                      </div>
                    </div>
                    <div class="pf-v5-l-grid__item pf-m-12-col">
                      <div class="pf-v5-c-card">
                        <div class="pf-v5-c-card__title">
                          <h2 class="pf-v5-c-card__title-text">Issue Tracker</h2>
                        </div>
                        <div class="pf-v5-c-card__body">Report issues with ${productName}.</div>
                        <div class="pf-v5-c-card__footer">
                          <a class="pf-v5-c-button pf-m-link pf-m-inline" href="https://github.com/keycloak/keycloak/issues">
                            Report an issue
                            <span class="pf-v5-c-button__icon pf-m-end">
                              <i class="fas fa-arrow-right" aria-hidden="true"></i>
                            </span>
                          </a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </#if>
            </div>
          </div>
        </section>
      </main>
    </div>
  </body>
</html>
