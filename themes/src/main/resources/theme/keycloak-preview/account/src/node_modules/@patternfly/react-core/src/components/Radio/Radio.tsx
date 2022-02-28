import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Radio/radio';
import { css, getModifier } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';

export interface RadioProps
  extends Omit<React.HTMLProps<HTMLInputElement>, 'disabled' | 'label' | 'onChange' | 'type'> {
  /** Additional classes added to the radio. */
  className?: string;
  /** Id of the radio. */
  id: string;
  /** Flag to show if the radio label is wrapped on small screen. */
  isLabelWrapped?: boolean;
  /** Flag to show if the radio label is shown before the radio button. */
  isLabelBeforeButton?: boolean;
  /** Flag to show if the radio is checked. */
  checked?: boolean;
  /** Flag to show if the radio is checked. */
  isChecked?: boolean;
  /** Flag to show if the radio is disabled. */
  isDisabled?: boolean;
  /** Flag to show if the radio selection is valid or invalid. */
  isValid?: boolean;
  /** Label text of the radio. */
  label?: React.ReactNode;
  /** Name for group of radios */
  name: string;
  /** A callback for when the radio selection changes. */
  onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
  /** Aria label for the radio. */
  'aria-label'?: string;
  /** Description text of the radio. */
  description?: React.ReactNode;
}

export class Radio extends React.Component<RadioProps> {
  static defaultProps: PickOptional<RadioProps> = {
    className: '',
    isDisabled: false,
    isValid: true,
    onChange: () => {}
  };

  constructor(props: RadioProps) {
    super(props);
    if (!props.label && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Radio:', 'Radio requires an aria-label to be specified');
    }
  }

  handleChange = (event: React.FormEvent<HTMLInputElement>) => {
    this.props.onChange(event.currentTarget.checked, event);
  };

  render() {
    const {
      'aria-label': ariaLabel,
      checked,
      className,
      defaultChecked,
      isLabelWrapped,
      isLabelBeforeButton,
      isChecked,
      isDisabled,
      isValid,
      label,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onChange,
      description,
      ...props
    } = this.props;

    const inputRendered = (
      <input
        {...props}
        className={css(styles.radioInput)}
        type="radio"
        onChange={this.handleChange}
        aria-invalid={!isValid}
        disabled={isDisabled}
        checked={checked || isChecked}
        {...(checked === undefined && { defaultChecked })}
        {...(!label && { 'aria-label': ariaLabel })}
      />
    );
    const labelRendered = !label ? null : isLabelWrapped ? (
      <span className={css(styles.radioLabel, getModifier(styles, isDisabled && ('disabled' as any)))}>{label}</span>
    ) : (
      <label
        className={css(styles.radioLabel, getModifier(styles, isDisabled && ('disabled' as any)))}
        htmlFor={props.id}
      >
        {label}
      </label>
    );

    const descRender = description ? <div className={css(styles.radioDescription)}>{description}</div> : null;
    const childrenRendered = isLabelBeforeButton ? (
      <>
        {labelRendered}
        {inputRendered}
        {descRender}
      </>
    ) : (
      <>
        {inputRendered}
        {labelRendered}
        {descRender}
      </>
    );

    return isLabelWrapped ? (
      <label className={css(styles.radio, className)} htmlFor={props.id}>
        {childrenRendered}
      </label>
    ) : (
      <div className={css(styles.radio, className)}>{childrenRendered}</div>
    );
  }
}
