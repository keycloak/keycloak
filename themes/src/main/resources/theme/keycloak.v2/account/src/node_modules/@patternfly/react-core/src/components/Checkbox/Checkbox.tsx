import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';
import { getDefaultOUIAId, getOUIAProps, OUIAProps } from '../../helpers';

export interface CheckboxProps
  extends Omit<React.HTMLProps<HTMLInputElement>, 'type' | 'onChange' | 'disabled' | 'label'>,
    OUIAProps {
  /** Additional classes added to the Checkbox. */
  className?: string;
  /** Flag to show if the Checkbox selection is valid or invalid. */
  isValid?: boolean;
  /** Flag to show if the Checkbox is disabled. */
  isDisabled?: boolean;
  /** Flag to show if the Checkbox is checked. If null, the checkbox will be indeterminate (partially checked). */
  isChecked?: boolean | null;
  checked?: boolean;
  /** A callback for when the Checkbox selection changes. */
  onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
  /** Label text of the checkbox. */
  label?: React.ReactNode;
  /** Id of the checkbox. */
  id: string;
  /** Aria-label of the checkbox. */
  'aria-label'?: string;
  /** Description text of the checkbox. */
  description?: React.ReactNode;
  /** Body text of the checkbox */
  body?: React.ReactNode;
  /** Sets the input wrapper component to render. Defaults to <div> */
  component?: React.ElementType;
}

// tslint:disable-next-line:no-empty
const defaultOnChange = () => {};

interface CheckboxState {
  ouiaStateId: string;
}

export class Checkbox extends React.Component<CheckboxProps, CheckboxState> {
  static displayName = 'Checkbox';
  static defaultProps: PickOptional<CheckboxProps> = {
    className: '',
    isValid: true,
    isDisabled: false,
    isChecked: false,
    onChange: defaultOnChange,
    ouiaSafe: true,
    component: 'div'
  };

  constructor(props: any) {
    super(props);
    this.state = {
      ouiaStateId: getDefaultOUIAId(Checkbox.displayName)
    };
  }

  private handleChange = (event: React.FormEvent<HTMLInputElement>): void => {
    this.props.onChange(event.currentTarget.checked, event);
  };

  render() {
    const {
      'aria-label': ariaLabel,
      className,
      onChange,
      isValid,
      isDisabled,
      isChecked,
      label,
      checked,
      defaultChecked,
      description,
      body,
      ouiaId,
      ouiaSafe,
      component: Component,
      ...props
    } = this.props;
    if (!props.id) {
      // eslint-disable-next-line no-console
      console.error('Checkbox:', 'id is required to make input accessible');
    }
    const checkedProps: { checked?: boolean; defaultChecked?: boolean } = {};
    if ([true, false].includes(checked) || isChecked === true) {
      checkedProps.checked = checked || isChecked;
    }
    if (onChange !== defaultOnChange) {
      checkedProps.checked = isChecked;
    }
    if ([false, true].includes(defaultChecked)) {
      checkedProps.defaultChecked = defaultChecked;
    }

    checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
    return (
      <Component className={css(styles.check, !label && styles.modifiers.standalone, className)}>
        <input
          {...props}
          className={css(styles.checkInput)}
          type="checkbox"
          onChange={this.handleChange}
          aria-invalid={!isValid}
          aria-label={ariaLabel}
          disabled={isDisabled}
          ref={elem => elem && (elem.indeterminate = isChecked === null)}
          {...checkedProps}
          {...getOUIAProps(Checkbox.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
        />
        {label && (
          <label className={css(styles.checkLabel, isDisabled && styles.modifiers.disabled)} htmlFor={props.id}>
            {label}
          </label>
        )}
        {description && <span className={css(styles.checkDescription)}>{description}</span>}
        {body && <span className={css(styles.checkBody)}>{body}</span>}
      </Component>
    );
  }
}
