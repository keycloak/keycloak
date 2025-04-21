import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';
import { AccordionContext } from './AccordionContext';

export interface AccordionProps extends React.HTMLProps<HTMLDListElement> {
  /** Content rendered inside the Accordion  */
  children?: React.ReactNode;
  /** Additional classes added to the Accordion  */
  className?: string;
  /** Adds accessible text to the Accordion */
  'aria-label'?: string;
  /** Heading level to use */
  headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  /** Flag to indicate whether use definition list or div */
  asDefinitionList?: boolean;
  /** Flag to indicate the accordion had a border */
  isBordered?: boolean;
  /** Display size variant. */
  displaySize?: 'default' | 'large';
}

export const Accordion: React.FunctionComponent<AccordionProps> = ({
  children = null,
  className = '',
  'aria-label': ariaLabel = '',
  headingLevel = 'h3',
  asDefinitionList = true,
  isBordered = false,
  displaySize = 'default',
  ...props
}: AccordionProps) => {
  const AccordionList: any = asDefinitionList ? 'dl' : 'div';
  return (
    <AccordionList
      className={css(
        styles.accordion,
        isBordered && styles.modifiers.bordered,
        displaySize === 'large' && styles.modifiers.displayLg,
        className
      )}
      aria-label={ariaLabel}
      {...props}
    >
      <AccordionContext.Provider
        value={{
          ContentContainer: asDefinitionList ? 'dd' : 'div',
          ToggleContainer: asDefinitionList ? 'dt' : headingLevel
        }}
      >
        {children}
      </AccordionContext.Provider>
    </AccordionList>
  );
};
Accordion.displayName = 'Accordion';
