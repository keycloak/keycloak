import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { Button, ButtonVariant } from '../Button';
import { WizardStep } from './Wizard';

export interface WizardFooterInternalProps {
  onNext: any;
  onBack: any;
  onClose: any;
  isValid: boolean;
  firstStep: boolean;
  activeStep: WizardStep;
  nextButtonText: string;
  backButtonText: string;
  cancelButtonText: string;
}

export const WizardFooterInternal: React.FunctionComponent<WizardFooterInternalProps> = ({
  onNext,
  onBack,
  onClose,
  isValid,
  firstStep,
  activeStep,
  nextButtonText,
  backButtonText,
  cancelButtonText
}: WizardFooterInternalProps) => (
  <footer className={css(styles.wizardFooter)}>
    <Button variant={ButtonVariant.primary} type="submit" onClick={onNext} isDisabled={!isValid}>
      {nextButtonText}
    </Button>
    {!activeStep.hideBackButton && (
      <Button variant={ButtonVariant.secondary} onClick={onBack} className={css(firstStep && 'pf-m-disabled')}>
        {backButtonText}
      </Button>
    )}
    {!activeStep.hideCancelButton && (
      <Button variant={ButtonVariant.link} onClick={onClose}>
        {cancelButtonText}
      </Button>
    )}
  </footer>
);
