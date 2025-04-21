import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';

export interface DropdownGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
  /** Checkboxes within group */
  children?: React.ReactNode;
  /** Additional classes added to the DropdownGroup control */
  className?: string;
  /** Group label */
  label?: React.ReactNode;
}

export const DropdownGroup: React.FunctionComponent<DropdownGroupProps> = ({
  children = null,
  className = '',
  label = '',
  ...props
}: DropdownGroupProps) => (
  <DropdownContext.Consumer>
    {({ sectionClass, sectionTitleClass, sectionComponent }) => {
      const SectionComponent = sectionComponent as any;
      return (
        <SectionComponent className={css(sectionClass, className)} {...props}>
          {label && (
            <h1 className={css(sectionTitleClass)} aria-hidden>
              {label}
            </h1>
          )}
          <ul role="none">{children}</ul>
        </SectionComponent>
      );
    }}
  </DropdownContext.Consumer>
);
DropdownGroup.displayName = 'DropdownGroup';
