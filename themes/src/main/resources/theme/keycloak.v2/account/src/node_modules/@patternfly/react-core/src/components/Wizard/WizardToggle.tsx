import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
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
  children?: React.ReactNode;
  /** Set to true to remove body padding */
  hasNoBodyPadding: boolean;
  /** If the nav is open */
  isNavOpen: boolean;
  /** Callback function for when the nav is toggled */
  onNavToggle: (isOpen: boolean) => void;
  /** The button's aria-label */
  'aria-label'?: string;
  /** Sets aria-labelledby on the main element */
  mainAriaLabelledBy?: string;
  /** The main's aria-label */
  mainAriaLabel?: string;
  /** If the wizard is in-page */
  isInPage?: boolean;
  /** @beta Flag indicating the wizard has a drawer for at least one of the wizard steps */
  hasDrawer?: boolean;
  /** @beta Flag indicating the wizard drawer is expanded */
  isDrawerExpanded?: boolean;
}

export const WizardToggle: React.FunctionComponent<WizardToggleProps> = ({
  isNavOpen,
  onNavToggle,
  nav,
  steps,
  activeStep,
  children,
  hasNoBodyPadding = false,
  'aria-label': ariaLabel = 'Wizard Toggle',
  mainAriaLabelledBy = null,
  mainAriaLabel = null,
  isInPage = true,
  hasDrawer,
  isDrawerExpanded
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
    <React.Fragment>
      <button
        onClick={() => onNavToggle(!isNavOpen)}
        className={css(styles.wizardToggle, isNavOpen && 'pf-m-expanded')}
        aria-label={ariaLabel}
        aria-expanded={isNavOpen}
      >
        <span className={css(styles.wizardToggleList)}>
          <span className={css(styles.wizardToggleListItem)}>
            <span className={css(styles.wizardToggleNum)}>{activeStepIndex}</span> {activeStepName}
            {activeStepSubName && <AngleRightIcon className={css(styles.wizardToggleSeparator)} aria-hidden="true" />}
          </span>
          {activeStepSubName && <span className={css(styles.wizardToggleListItem)}>{activeStepSubName}</span>}
        </span>
        <span className={css(styles.wizardToggleIcon)}>
          <CaretDownIcon aria-hidden="true" />
        </span>
      </button>
      <div className={css(styles.wizardOuterWrap)}>
        <div className={css(styles.wizardInnerWrap)}>
          {nav(isNavOpen)}
          <WizardBody
            mainComponent={isInPage ? 'div' : 'main'}
            aria-label={mainAriaLabel}
            aria-labelledby={mainAriaLabelledBy}
            hasNoBodyPadding={hasNoBodyPadding}
            activeStep={activeStep}
            isDrawerExpanded={isDrawerExpanded}
            hasDrawer={hasDrawer}
          >
            {hasDrawer && !isDrawerExpanded && activeStep.drawerToggleButton}
            {activeStep.component}
          </WizardBody>
        </div>
        {children}
      </div>
    </React.Fragment>
  );
};
WizardToggle.displayName = 'WizardToggle';
