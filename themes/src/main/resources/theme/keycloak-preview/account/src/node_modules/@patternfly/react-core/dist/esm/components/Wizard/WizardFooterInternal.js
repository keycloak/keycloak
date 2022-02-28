import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { Button, ButtonVariant } from '../Button';
export const WizardFooterInternal = ({
  onNext,
  onBack,
  onClose,
  isValid,
  firstStep,
  activeStep,
  nextButtonText,
  backButtonText,
  cancelButtonText
}) => React.createElement("footer", {
  className: css(styles.wizardFooter)
}, React.createElement(Button, {
  variant: ButtonVariant.primary,
  type: "submit",
  onClick: onNext,
  isDisabled: !isValid
}, nextButtonText), !activeStep.hideBackButton && React.createElement(Button, {
  variant: ButtonVariant.secondary,
  onClick: onBack,
  className: css(firstStep && 'pf-m-disabled')
}, backButtonText), !activeStep.hideCancelButton && React.createElement(Button, {
  variant: ButtonVariant.link,
  onClick: onClose
}, cancelButtonText));
WizardFooterInternal.propTypes = {
  onNext: _pt.any.isRequired,
  onBack: _pt.any.isRequired,
  onClose: _pt.any.isRequired,
  isValid: _pt.bool.isRequired,
  firstStep: _pt.bool.isRequired,
  activeStep: _pt.any.isRequired,
  nextButtonText: _pt.string.isRequired,
  backButtonText: _pt.string.isRequired,
  cancelButtonText: _pt.string.isRequired
};
//# sourceMappingURL=WizardFooterInternal.js.map