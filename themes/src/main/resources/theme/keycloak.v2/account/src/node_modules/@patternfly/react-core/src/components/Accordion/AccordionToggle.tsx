import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { AccordionContext } from './AccordionContext';

export interface AccordionToggleProps
  extends React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement> {
  /** Content rendered inside the Accordion toggle  */
  children?: React.ReactNode;
  /** Additional classes added to the Accordion Toggle  */
  className?: string;
  /** Flag to show if the expanded content of the Accordion item is visible  */
  isExpanded?: boolean;
  /** Identify the Accordion toggle number  */
  id: string;
  /** Container to override the default for toggle */
  component?: React.ElementType;
}

export const AccordionToggle: React.FunctionComponent<AccordionToggleProps> = ({
  className = '',
  id,
  isExpanded = false,
  children = null,
  component,
  ...props
}: AccordionToggleProps) => (
  <AccordionContext.Consumer>
    {({ ToggleContainer }) => {
      const Container = component || ToggleContainer;
      return (
        <Container>
          <button
            id={id}
            className={css(styles.accordionToggle, isExpanded && styles.modifiers.expanded, className)}
            aria-expanded={isExpanded}
            type="button"
            {...props}
          >
            <span className={css(styles.accordionToggleText)}>{children}</span>
            <span className={css(styles.accordionToggleIcon)}>
              <AngleRightIcon />
            </span>
          </button>
        </Container>
      );
    }}
  </AccordionContext.Consumer>
);
AccordionToggle.displayName = 'AccordionToggle';
