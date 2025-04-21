import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
import { WizardDrawerWrapper } from './WizardDrawerWrapper';
import { Drawer, DrawerContent } from '../Drawer';
import { WizardStep } from './Wizard';

export interface WizardBodyProps {
  /** Anything that can be rendered in the Wizard body */
  children: any;
  /** Set to true to remove the default body padding */
  hasNoBodyPadding: boolean;
  /** An aria-label to use for the main element */
  'aria-label'?: string;
  /** Sets the aria-labelledby attribute for the main element */
  'aria-labelledby': string;
  /** Component used as the primary content container */
  mainComponent?: React.ElementType;
  /** The currently active WizardStep */
  activeStep: WizardStep;
  hasDrawer?: boolean;
  /** Flag indicating the wizard drawer is expanded */
  isDrawerExpanded?: boolean;
  /** Callback function for when the drawer is toggled */
}

export const WizardBody: React.FunctionComponent<WizardBodyProps> = ({
  children,
  hasNoBodyPadding = false,
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledBy,
  mainComponent = 'div',
  hasDrawer,
  isDrawerExpanded,
  activeStep
}: WizardBodyProps) => {
  const MainComponent = mainComponent;
  return (
    <MainComponent aria-label={ariaLabel} aria-labelledby={ariaLabelledBy} className={css(styles.wizardMain)}>
      <WizardDrawerWrapper
        hasDrawer={hasDrawer && activeStep.drawerPanelContent}
        wrapper={(children: React.ReactNode) => (
          <Drawer isInline isExpanded={isDrawerExpanded}>
            <DrawerContent panelContent={activeStep.drawerPanelContent}>{children}</DrawerContent>
          </Drawer>
        )}
      >
        <div className={css(styles.wizardMainBody, hasNoBodyPadding && styles.modifiers.noPadding)}>{children}</div>
      </WizardDrawerWrapper>
    </MainComponent>
  );
};
WizardBody.displayName = 'WizardBody';
