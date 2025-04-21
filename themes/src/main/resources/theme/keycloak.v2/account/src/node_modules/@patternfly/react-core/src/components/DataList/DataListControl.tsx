import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';

export interface DataListControlProps extends React.HTMLProps<HTMLDivElement> {
  /** Children of the data list control */
  children?: React.ReactNode;
  /** Additional classes added to the DataList item control */
  className?: string;
}

export const DataListControl: React.FunctionComponent<DataListControlProps> = ({
  children,
  className = '',
  ...props
}: DataListControlProps) => (
  <div className={css(styles.dataListItemControl, className)} {...props}>
    {children}
  </div>
);
DataListControl.displayName = 'DataListControl';
