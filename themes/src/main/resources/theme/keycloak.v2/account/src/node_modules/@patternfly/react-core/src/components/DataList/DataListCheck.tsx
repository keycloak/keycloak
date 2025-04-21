import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';

export interface DataListCheckProps extends Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'checked'> {
  /** Additional classes added to the DataList item checkbox */
  className?: string;
  /** Flag to show if the DataList checkbox selection is valid or invalid */
  isValid?: boolean;
  /** Flag to show if the DataList checkbox is disabled */
  isDisabled?: boolean;
  /** Flag to show if the DataList checkbox is checked when it is controlled by React state. Both isChecked and checked are valid,
   * but only use one.
   * To make the DataList checkbox uncontrolled, instead use the defaultChecked prop, but do not use both.
   */
  isChecked?: boolean;
  /** Flag to show if the DataList checkbox is checked when it is controlled by React state. Both isChecked and checked are valid,
   * but only use one.
   * To make the DataList checkbox uncontrolled, instead use the defaultChecked prop, but do not use both.
   */
  checked?: boolean;
  /** Flag to set default value of DataList checkbox when it is uncontrolled by React state.
   * To make the DataList checkbox controlled, instead use the isChecked prop, but do not use both.
   */
  defaultChecked?: boolean;
  /** A callback for when the DataList checkbox selection changes */
  onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
  /** Aria-labelledby of the DataList checkbox */
  'aria-labelledby': string;
  /** Flag to indicate if other controls are used in the DataListItem */
  otherControls?: boolean;
}

export const DataListCheck: React.FunctionComponent<DataListCheckProps> = ({
  className = '',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onChange = (checked: boolean, event: React.FormEvent<HTMLInputElement>) => {},
  isValid = true,
  isDisabled = false,
  isChecked = null,
  checked = null,
  defaultChecked,
  otherControls = false,
  ...props
}: DataListCheckProps) => {
  const check = (
    <div className={css(styles.dataListCheck)}>
      <input
        {...props}
        type="checkbox"
        onChange={event => onChange(event.currentTarget.checked, event)}
        aria-invalid={!isValid}
        disabled={isDisabled}
        {...([true, false].includes(defaultChecked) && { defaultChecked })}
        {...(![true, false].includes(defaultChecked) && { checked: isChecked || checked })}
      />
    </div>
  );
  return (
    <React.Fragment>
      {!otherControls && <div className={css(styles.dataListItemControl, className)}>{check}</div>}
      {otherControls && check}
    </React.Fragment>
  );
};
DataListCheck.displayName = 'DataListCheck';
