import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { DataListWrapModifier } from './DataList';

export interface DataListItemRowProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
  /** Content rendered inside the DataListItemRow  */
  children: React.ReactNode;
  /** Additional classes added to the DataListItemRow */
  className?: string;
  /** Id for the row item */
  rowid?: string;
  /** Determines which wrapping modifier to apply to the DataListItemRow */
  wrapModifier?: DataListWrapModifier | 'nowrap' | 'truncate' | 'breakWord';
}

export const DataListItemRow: React.FunctionComponent<DataListItemRowProps> = ({
  children,
  className = '',
  rowid = '',
  wrapModifier = null,
  ...props
}: DataListItemRowProps) => (
  <div className={css(styles.dataListItemRow, className, wrapModifier && styles.modifiers[wrapModifier])} {...props}>
    {React.Children.map(
      children,
      child =>
        React.isValidElement(child) &&
        React.cloneElement(child as React.ReactElement<any>, {
          rowid
        })
    )}
  </div>
);
DataListItemRow.displayName = 'DataListItemRow';
