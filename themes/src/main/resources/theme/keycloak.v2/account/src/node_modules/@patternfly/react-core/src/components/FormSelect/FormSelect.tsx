import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';
import { ValidatedOptions } from '../../helpers/constants';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';

export interface FormSelectProps
  extends Omit<React.HTMLProps<HTMLSelectElement>, 'onChange' | 'onBlur' | 'onFocus' | 'disabled'>,
    OUIAProps {
  /** content rendered inside the FormSelect */
  children: React.ReactNode;
  /** additional classes added to the FormSelect control */
  className?: string;
  /** value of selected option */
  value?: any;
  /** Value to indicate if the select is modified to show that validation state.
   * If set to success, select will be modified to indicate valid state.
   * If set to error, select will be modified to indicate error state.
   */
  validated?: 'success' | 'warning' | 'error' | 'default';
  /** Flag indicating the FormSelect is disabled */
  isDisabled?: boolean;
  /** Sets the FormSelect required. */
  isRequired?: boolean;
  /** Use the external file instead of a data URI */
  isIconSprite?: boolean;
  /** Optional callback for updating when selection loses focus */
  onBlur?: (event: React.FormEvent<HTMLSelectElement>) => void;
  /** Optional callback for updating when selection gets focus */
  onFocus?: (event: React.FormEvent<HTMLSelectElement>) => void;
  /** Optional callback for updating when selection changes */
  onChange?: (value: string, event: React.FormEvent<HTMLSelectElement>) => void;
  /** Custom flag to show that the FormSelect requires an associated id or aria-label. */
  'aria-label'?: string;
}

export class FormSelect extends React.Component<FormSelectProps, { ouiaStateId: string }> {
  static displayName = 'FormSelect';
  constructor(props: FormSelectProps) {
    super(props);
    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('FormSelect requires either an id or aria-label to be specified');
    }
    this.state = {
      ouiaStateId: getDefaultOUIAId(FormSelect.displayName, props.validated)
    };
  }

  static defaultProps: PickOptional<FormSelectProps> = {
    className: '',
    value: '',
    validated: 'default',
    isDisabled: false,
    isRequired: false,
    isIconSprite: false,
    onBlur: (): any => undefined,
    onFocus: (): any => undefined,
    onChange: (): any => undefined,
    ouiaSafe: true
  };

  handleChange = (event: any) => {
    this.props.onChange(event.currentTarget.value, event);
  };

  render() {
    const {
      children,
      className,
      value,
      validated,
      isDisabled,
      isRequired,
      isIconSprite,
      ouiaId,
      ouiaSafe,
      ...props
    } = this.props;
    /* find selected option and get placeholder flag */
    const selectedOption = React.Children.toArray(children).find((option: any) => option.props.value === value) as any;
    const isSelectedPlaceholder = selectedOption && selectedOption.props.isPlaceholder;
    return (
      <select
        {...props}
        className={css(
          styles.formControl,
          isIconSprite && styles.modifiers.iconSprite,
          className,
          validated === ValidatedOptions.success && styles.modifiers.success,
          validated === ValidatedOptions.warning && styles.modifiers.warning,
          isSelectedPlaceholder && styles.modifiers.placeholder
        )}
        aria-invalid={validated === ValidatedOptions.error}
        {...getOUIAProps(FormSelect.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
        onChange={this.handleChange}
        disabled={isDisabled}
        required={isRequired}
        value={value}
      >
        {children}
      </select>
    );
  }
}
