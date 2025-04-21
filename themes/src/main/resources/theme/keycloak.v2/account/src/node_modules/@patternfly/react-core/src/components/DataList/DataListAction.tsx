import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { formatBreakpointMods } from '../../helpers/util';

export interface DataListActionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
  /** Content rendered as DataList Action  (e.g <Button> or <Dropdown>) */
  children: React.ReactNode;
  /** Additional classes added to the DataList Action */
  className?: string;
  /** Identify the DataList toggle number */
  id: string;
  /** Adds accessible text to the DataList Action */
  'aria-labelledby': string;
  /** Adds accessible text to the DataList Action */
  'aria-label': string;
  /** What breakpoints to hide/show the data list action */
  visibility?: {
    default?: 'hidden' | 'visible';
    sm?: 'hidden' | 'visible';
    md?: 'hidden' | 'visible';
    lg?: 'hidden' | 'visible';
    xl?: 'hidden' | 'visible';
    '2xl'?: 'hidden' | 'visible';
  };
  /** Flag to indicate that the action is a plain button (e.g. kebab dropdown toggle) so that styling is applied to align the button */
  isPlainButtonAction?: boolean;
}

export const DataListAction: React.FunctionComponent<DataListActionProps> = ({
  children,
  className,
  visibility,
  /* eslint-disable @typescript-eslint/no-unused-vars */
  id,
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledBy,
  isPlainButtonAction,
  /* eslint-enable @typescript-eslint/no-unused-vars */
  ...props
}: DataListActionProps) => (
  <div className={css(styles.dataListItemAction, formatBreakpointMods(visibility, styles), className)} {...props}>
    {isPlainButtonAction ? <div className={css(styles.dataListAction)}>{children}</div> : children}
  </div>
);
DataListAction.displayName = 'DataListAction';
