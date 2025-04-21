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
  nextButtonText: React.ReactNode;
  backButtonText: React.ReactNode;
  cancelButtonText: React.ReactNode;
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
      <Button variant={ButtonVariant.secondary} onClick={onBack} isDisabled={firstStep}>
        {backButtonText}
      </Button>
    )}
    {!activeStep.hideCancelButton && (
      <div className={styles.wizardFooterCancel}>
        <Button variant={ButtonVariant.link} onClick={onClose}>
          {cancelButtonText}
        </Button>
      </div>
    )}
  </footer>
);
WizardFooterInternal.displayName = 'WizardFooterInternal';
