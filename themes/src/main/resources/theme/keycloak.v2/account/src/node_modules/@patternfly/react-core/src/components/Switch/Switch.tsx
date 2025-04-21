import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Switch/switch';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { getUniqueId } from '../../helpers/util';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';

export interface SwitchProps
  extends Omit<React.HTMLProps<HTMLInputElement>, 'type' | 'onChange' | 'disabled' | 'label'>,
    OUIAProps {
  /** id for the label. */
  id?: string;
  /** Additional classes added to the switch */
  className?: string;
  /** Text value for the visible label when on */
  label?: React.ReactNode;
  /** Text value for the visible label when off */
  labelOff?: React.ReactNode;
  /** Flag to show if the switch is checked when it is controlled by React state.
   * To make the switch uncontrolled instead use the defaultChecked prop, but do not use both.
   */
  isChecked?: boolean;
  /** Flag to set the default checked value of the switch when it is uncontrolled by React state.
   * To make the switch controlled instead use the isChecked prop, but do not use both.
   */
  defaultChecked?: boolean;
  /** Flag to show if the switch has a check icon. */
  hasCheckIcon?: boolean;
  /** Flag to show if the switch is disabled. */
  isDisabled?: boolean;
  /** A callback for when the switch selection changes. (isChecked, event) => {} */
  onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
  /** Adds accessible text to the switch, and should describe the isChecked="true" state. When label is defined, aria-label should be set to the text string that is visible when isChecked is true. */
  'aria-label'?: string;
  /** Flag to reverse the layout of toggle and label (toggle on right). */
  isReversed?: boolean;
}

export class Switch extends React.Component<SwitchProps & OUIAProps, { ouiaStateId: string }> {
  static displayName = 'Switch';
  id: string;

  static defaultProps: SwitchProps = {
    isChecked: true,
    isDisabled: false,
    isReversed: false,
    'aria-label': '',
    onChange: () => undefined as any
  };

  constructor(props: SwitchProps & OUIAProps) {
    super(props);
    if (!props.label && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Switch: Switch requires either a label or an aria-label to be specified');
    }

    this.id = props.id || getUniqueId();
    this.state = {
      ouiaStateId: getDefaultOUIAId(Switch.displayName)
    };
  }

  render() {
    const {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      id,
      className,
      label,
      labelOff,
      isChecked,
      defaultChecked,
      hasCheckIcon,
      isDisabled,
      onChange,
      isReversed,
      ouiaId,
      ouiaSafe,
      ...props
    } = this.props;

    const isAriaLabelledBy = props['aria-label'] === '';
    return (
      <label
        className={css(styles.switch, isReversed && styles.modifiers.reverse, className)}
        htmlFor={this.id}
        {...getOUIAProps(Switch.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
      >
        <input
          id={this.id}
          className={css(styles.switchInput)}
          type="checkbox"
          onChange={event => onChange(event.target.checked, event)}
          {...([true, false].includes(defaultChecked) && { defaultChecked })}
          {...(![true, false].includes(defaultChecked) && { checked: isChecked })}
          disabled={isDisabled}
          aria-labelledby={isAriaLabelledBy ? `${this.id}-on` : null}
          {...props}
        />
        {label !== undefined ? (
          <React.Fragment>
            <span className={css(styles.switchToggle)}>
              {hasCheckIcon && (
                <span className={css(styles.switchToggleIcon)} aria-hidden="true">
                  <CheckIcon noVerticalAlign />
                </span>
              )}
            </span>
            <span
              className={css(styles.switchLabel, styles.modifiers.on)}
              id={isAriaLabelledBy ? `${this.id}-on` : null}
              aria-hidden="true"
            >
              {label}
            </span>
            <span
              className={css(styles.switchLabel, styles.modifiers.off)}
              id={isAriaLabelledBy ? `${this.id}-off` : null}
              aria-hidden="true"
            >
              {labelOff !== undefined ? labelOff : label}
            </span>
          </React.Fragment>
        ) : (
          <span className={css(styles.switchToggle)}>
            <div className={css(styles.switchToggleIcon)} aria-hidden="true">
              <CheckIcon noVerticalAlign />
            </div>
          </span>
        )}
      </label>
    );
  }
}
