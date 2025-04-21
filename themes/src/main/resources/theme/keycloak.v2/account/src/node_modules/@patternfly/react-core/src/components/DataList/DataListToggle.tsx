import * as React from 'react';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { Button, ButtonVariant } from '../Button';

export interface DataListToggleProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the DataList cell */
  className?: string;
  /** Flag to show if the expanded content of the DataList item is visible */
  isExpanded?: boolean;
  /** Identify the DataList toggle number */
  id: string;
  /** Id for the row */
  rowid?: string;
  /** Adds accessible text to the DataList toggle */
  'aria-labelledby'?: string;
  /** Adds accessible text to the DataList toggle */
  'aria-label'?: string;
  /** Allows users of some screen readers to shift focus to the controlled element. Should be used when the controlled contents are not adjacent to the toggle that controls them. */
  'aria-controls'?: string;
}

export const DataListToggle: React.FunctionComponent<DataListToggleProps> = ({
  className = '',
  isExpanded = false,
  'aria-controls': ariaControls = '',
  'aria-label': ariaLabel = 'Details',
  rowid = '',
  id,
  ...props
}: DataListToggleProps) => (
  <div className={css(styles.dataListItemControl, className)} {...props}>
    <div className={css(styles.dataListToggle)}>
      <Button
        id={id}
        variant={ButtonVariant.plain}
        aria-controls={ariaControls !== '' && ariaControls}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabel !== 'Details' ? null : `${rowid} ${id}`}
        aria-expanded={isExpanded}
      >
        <div className={css(styles.dataListToggleIcon)}>
          <AngleRightIcon />
        </div>
      </Button>
    </div>
  </div>
);
DataListToggle.displayName = 'DataListToggle';
