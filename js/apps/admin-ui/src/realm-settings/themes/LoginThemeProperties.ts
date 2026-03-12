// Keycloak v2 Login Theme Properties
// Based on theme/keycloak.v2/login/theme.properties

export const loginThemeProperties = {
  // Form classes
  kcFormGroupClass: "pf-v6-c-form__group",
  kcFormGroupLabelClass: "pf-v6-c-form__group-label pf-v6-u-pb-xs",
  kcFormLabelClass: "pf-v6-c-form__label",
  kcFormLabelTextClass: "pf-v6-c-form__label-text",
  kcLabelClass: "pf-v6-c-form__label",
  kcInputClass: "pf-v6-c-form-control",
  kcInputGroup: "pf-v6-c-input-group",
  kcFormHelperTextClass: "pf-v6-c-form__helper-text",
  kcInputHelperTextClass:
    "pf-v6-c-helper-text pf-v6-u-display-flex pf-v6-u-justify-content-space-between",
  kcInputHelperTextItemClass: "pf-v6-c-helper-text__item",
  kcInputHelperTextItemTextClass: "pf-v6-c-helper-text__item-text",
  kcInputGroupItemClass: "pf-v6-c-input-group__item",
  kcFill: "pf-m-fill",
  kcError: "pf-m-error",

  // Checkbox classes
  kcCheckboxClass: "pf-v6-c-check",
  kcCheckboxInputClass: "pf-v6-c-check__input",
  kcCheckboxLabelClass: "pf-v6-c-check__label",
  kcCheckboxLabelRequiredClass: "pf-v6-c-check__label-required",

  // Form control utilities
  kcInputRequiredClass: "pf-v6-c-form__label-required",
  kcInputErrorMessageClass:
    "pf-v6-c-helper-text__item-text pf-m-error kc-feedback-text",
  kcFormControlUtilClass: "pf-v6-c-form-control__utilities",
  kcInputErrorIconStatusClass: "pf-v6-c-form-control__icon pf-m-status",
  kcInputErrorIconClass: "fas fa-exclamation-circle",

  // Alert classes
  kcAlertClass: "pf-v6-c-alert pf-m-inline pf-v6-u-mb-md",
  kcAlertIconClass: "pf-v6-c-alert__icon",
  kcAlertTitleClass: "pf-v6-c-alert__title",
  kcAlertDescriptionClass: "pf-v6-c-alert__description",

  // Password visibility
  kcFormPasswordVisibilityButtonClass: "pf-v6-c-button pf-m-control",
  kcFormPasswordVisibilityIconShow: "fa-eye fas",
  kcFormPasswordVisibilityIconHide: "fa-eye-slash fas",
  kcFormControlToggleIcon: "pf-v6-c-form-control__toggle-icon",

  // Form actions
  kcFormActionGroupClass: "pf-v6-c-form__actions pf-v6-u-pt-xs",
  kcFormReadOnlyClass: "pf-m-readonly",

  // Button classes
  kcButtonClass: "pf-v6-c-button",
  kcButtonPrimaryClass: "pf-v6-c-button pf-m-primary",
  kcButtonSecondaryClass: "pf-v6-c-button pf-m-secondary",
  kcButtonBlockClass: "pf-m-block",
  kcButtonLinkClass: "pf-v6-c-button pf-m-link",

  // Login layout classes
  kcLogin: "pf-v6-c-login",
  kcLoginContainer: "pf-v6-c-login__container",
  kcLoginMain: "pf-v6-c-login__main",
  kcLoginMainHeader: "pf-v6-c-login__main-header",
  kcLoginMainFooter: "pf-v6-c-login__main-footer",
  kcLoginMainFooterBand: "pf-v6-c-login__main-footer-band",
  kcLoginMainFooterBandItem: "pf-v6-c-login__main-footer-band-item",
  kcLoginMainFooterHelperText: "pf-v6-u-font-size-sm pf-v6-u-color-200",
  kcLoginMainTitle: "pf-v6-c-title pf-m-3xl",
  kcLoginMainHeaderUtilities: "pf-v6-c-login__main-header-utilities",
  kcLoginMainBody: "pf-v6-c-login__main-body",

  // Form and card classes
  kcLoginClass: "pf-v6-c-login__main",
  kcFormClass: "pf-v6-c-form pf-v6-u-w-100",
  kcFormCardClass: "card-pf",

  // Feedback icons
  kcFeedbackErrorIcon: "fa fa-fw fa-exclamation-circle",
  kcFeedbackWarningIcon: "fa fa-fw fa-exclamation-triangle",
  kcFeedbackSuccessIcon: "fa fa-fw fa-check-circle",
  kcFeedbackInfoIcon: "fa fa-fw fa-info-circle",

  // Dark mode
  kcDarkModeClass: "pf-v6-theme-dark",

  // HTML and Body classes
  kcHtmlClass: "login-pf",
  kcBodyClass: "",

  // Content wrapper
  kcContentWrapperClass: "pf-v6-u-mb-md-on-md",
};

export type LoginThemeProperties = typeof loginThemeProperties;
