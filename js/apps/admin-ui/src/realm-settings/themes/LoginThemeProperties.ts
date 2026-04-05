// Keycloak v2 Login Theme Properties
// Based on theme/keycloak.v2/login/theme.properties

export const loginThemeProperties = {
  // Form classes
  kcFormGroupClass: "pf-v5-c-form__group",
  kcFormGroupLabelClass: "pf-v5-c-form__group-label pf-v5-u-pb-xs",
  kcFormLabelClass: "pf-v5-c-form__label",
  kcFormLabelTextClass: "pf-v5-c-form__label-text",
  kcLabelClass: "pf-v5-c-form__label",
  kcInputClass: "pf-v5-c-form-control",
  kcInputGroup: "pf-v5-c-input-group",
  kcFormHelperTextClass: "pf-v5-c-form__helper-text",
  kcInputHelperTextClass:
    "pf-v5-c-helper-text pf-v5-u-display-flex pf-v5-u-justify-content-space-between",
  kcInputHelperTextItemClass: "pf-v5-c-helper-text__item",
  kcInputHelperTextItemTextClass: "pf-v5-c-helper-text__item-text",
  kcInputGroupItemClass: "pf-v5-c-input-group__item",
  kcFill: "pf-m-fill",
  kcError: "pf-m-error",

  // Checkbox classes
  kcCheckboxClass: "pf-v5-c-check",
  kcCheckboxInputClass: "pf-v5-c-check__input",
  kcCheckboxLabelClass: "pf-v5-c-check__label",
  kcCheckboxLabelRequiredClass: "pf-v5-c-check__label-required",

  // Form control utilities
  kcInputRequiredClass: "pf-v5-c-form__label-required",
  kcInputErrorMessageClass:
    "pf-v5-c-helper-text__item-text pf-m-error kc-feedback-text",
  kcFormControlUtilClass: "pf-v5-c-form-control__utilities",
  kcInputErrorIconStatusClass: "pf-v5-c-form-control__icon pf-m-status",
  kcInputErrorIconClass: "fas fa-exclamation-circle",

  // Alert classes
  kcAlertClass: "pf-v5-c-alert pf-m-inline pf-v5-u-mb-md",
  kcAlertIconClass: "pf-v5-c-alert__icon",
  kcAlertTitleClass: "pf-v5-c-alert__title",
  kcAlertDescriptionClass: "pf-v5-c-alert__description",

  // Password visibility
  kcFormPasswordVisibilityButtonClass: "pf-v5-c-button pf-m-control",
  kcFormPasswordVisibilityIconShow: "fa-eye fas",
  kcFormPasswordVisibilityIconHide: "fa-eye-slash fas",
  kcFormControlToggleIcon: "pf-v5-c-form-control__toggle-icon",

  // Form actions
  kcFormActionGroupClass: "pf-v5-c-form__actions pf-v5-u-pt-xs",
  kcFormReadOnlyClass: "pf-m-readonly",

  // Button classes
  kcButtonClass: "pf-v5-c-button",
  kcButtonPrimaryClass: "pf-v5-c-button pf-m-primary",
  kcButtonSecondaryClass: "pf-v5-c-button pf-m-secondary",
  kcButtonBlockClass: "pf-m-block",
  kcButtonLinkClass: "pf-v5-c-button pf-m-link",

  // Login layout classes
  kcLogin: "pf-v5-c-login",
  kcLoginContainer: "pf-v5-c-login__container",
  kcLoginMain: "pf-v5-c-login__main",
  kcLoginMainHeader: "pf-v5-c-login__main-header",
  kcLoginMainFooter: "pf-v5-c-login__main-footer",
  kcLoginMainFooterBand: "pf-v5-c-login__main-footer-band",
  kcLoginMainFooterBandItem: "pf-v5-c-login__main-footer-band-item",
  kcLoginMainFooterHelperText: "pf-v5-u-font-size-sm pf-v5-u-color-200",
  kcLoginMainTitle: "pf-v5-c-title pf-m-3xl",
  kcLoginMainHeaderUtilities: "pf-v5-c-login__main-header-utilities",
  kcLoginMainBody: "pf-v5-c-login__main-body",

  // Form and card classes
  kcLoginClass: "pf-v5-c-login__main",
  kcFormClass: "pf-v5-c-form pf-v5-u-w-100",
  kcFormCardClass: "card-pf",

  // Feedback icons
  kcFeedbackErrorIcon: "fa fa-fw fa-exclamation-circle",
  kcFeedbackWarningIcon: "fa fa-fw fa-exclamation-triangle",
  kcFeedbackSuccessIcon: "fa fa-fw fa-check-circle",
  kcFeedbackInfoIcon: "fa fa-fw fa-info-circle",

  // Dark mode
  kcDarkModeClass: "pf-v5-theme-dark",

  // HTML and Body classes
  kcHtmlClass: "login-pf",
  kcBodyClass: "",

  // Content wrapper
  kcContentWrapperClass: "pf-v5-u-mb-md-on-md",
};

export type LoginThemeProperties = typeof loginThemeProperties;
