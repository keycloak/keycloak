import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';

export interface DropdownToggleCheckboxProps
  extends Omit<React.HTMLProps<HTMLInputElement>, 'type' | 'onChange' | 'disabled' | 'checked'> {
  /** Additional classes added to the DropdownToggleCheckbox */
  className?: string;
  /** Flag to show if the checkbox selection is valid or invalid */
  isValid?: boolean;
  /** Flag to show if the checkbox is disabled */
  isDisabled?: boolean;
  /** Flag to show if the checkbox is checked */
  isChecked?: boolean | null;
  /** Alternate Flag to show if the checkbox is checked */
  checked?: boolean | null;
  /** A callback for when the checkbox selection changes */
  onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
  /** Element to be rendered inside the <span> */
  children?: React.ReactNode;
  /** Id of the checkbox */
  id: string;
  /** Aria-label of the checkbox */
  'aria-label': string;
}

export class DropdownToggleCheckbox extends React.Component<DropdownToggleCheckboxProps> {
  static defaultProps: PickOptional<DropdownToggleCheckboxProps> = {
    className: '',
    isValid: true,
    isDisabled: false,
    onChange: () => undefined as any
  };

  handleChange = (event: React.FormEvent<HTMLInputElement>) => {
    this.props.onChange((event.target as HTMLInputElement).checked, event);
  };

  calculateChecked = () => {
    const { isChecked, checked } = this.props;
    return isChecked !== undefined ? isChecked : checked;
  };

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { className, onChange, isValid, isDisabled, isChecked, ref, checked, children, ...props } = this.props;
    const text = children && (
      <span className={css(styles.dropdownToggleText, className)} aria-hidden="true" id={`${props.id}-text`}>
        {children}
      </span>
    );
    return (
      <label className={css(styles.dropdownToggleCheck, className)} htmlFor={props.id}>
        <input
          {...props}
          {...(this.calculateChecked() !== undefined && { onChange: this.handleChange })}
          type="checkbox"
          ref={ref as any}
          aria-invalid={!isValid}
          disabled={isDisabled}
          checked={this.calculateChecked()}
        />
        {text}
      </label>
    );
  }
}
