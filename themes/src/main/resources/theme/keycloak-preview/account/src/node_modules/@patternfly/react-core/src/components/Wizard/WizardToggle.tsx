import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { WizardStep } from './Wizard';
import { WizardBody } from './WizardBody';

export interface WizardToggleProps {
  /** Function that returns the WizardNav component */
  nav: (isWizardNavOpen: boolean) => React.ReactElement;
  /** The wizard steps */
  steps: WizardStep[];
  /** The currently active WizardStep */
  activeStep: WizardStep;
  /** The WizardFooter */
  children: React.ReactNode;
  /** Set to false to remove body padding */
  hasBodyPadding: boolean;
  /** If the nav is open */
  isNavOpen: boolean;
  /** Callback function for when the nav is toggled */
  onNavToggle: (isOpen: boolean) => void;
}

export const WizardToggle: React.FunctionComponent<WizardToggleProps> = ({
  isNavOpen,
  onNavToggle,
  nav,
  steps,
  activeStep,
  children,
  hasBodyPadding = true
}: WizardToggleProps) => {
  let activeStepIndex;
  let activeStepName;
  let activeStepSubName;
  for (let i = 0; i < steps.length; i++) {
    if ((activeStep.id && steps[i].id === activeStep.id) || steps[i].name === activeStep.name) {
      activeStepIndex = i + 1;
      activeStepName = steps[i].name;
      break;
    } else if (steps[i].steps) {
      for (const step of steps[i].steps) {
        if ((activeStep.id && step.id === activeStep.id) || step.name === activeStep.name) {
          activeStepIndex = i + 1;
          activeStepName = steps[i].name;
          activeStepSubName = step.name;
          break;
        }
      }
    }
  }
  return (
    <>
      <button
        onClick={() => onNavToggle(!isNavOpen)}
        className={css(styles.wizardToggle, isNavOpen && 'pf-m-expanded')}
        aria-expanded={isNavOpen}
      >
        <ol className={css(styles.wizardToggleList)}>
          <li className={css(styles.wizardToggleListItem)}>
            <span className={css(styles.wizardToggleNum)}>{activeStepIndex}</span> {activeStepName}
            {activeStepSubName && <AngleRightIcon className={css(styles.wizardToggleSeparator)} aria-hidden="true" />}
          </li>
          {activeStepSubName && <li className={css(styles.wizardToggleListItem)}>{activeStepSubName}</li>}
        </ol>
        <CaretDownIcon className={css(styles.wizardToggleIcon)} aria-hidden="true" />
      </button>
      <div className={css(styles.wizardOuterWrap)}>
        <div className={css(styles.wizardInnerWrap)}>
          {nav(isNavOpen)}
          <WizardBody hasBodyPadding={hasBodyPadding}>{activeStep.component}</WizardBody>
        </div>
        {children}
      </div>
    </>
  );
};
