import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as checkStyles } from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import { SelectConsumer, KeyTypes } from './selectConstants';

export interface CheckboxSelectOptionProps extends React.HTMLProps<HTMLLabelElement> {
  /** Optional alternate display for the option */
  children?: React.ReactNode;
  /** Additional classes added to the Select Option */
  className?: string;
  /** Internal index of the option */
  index?: number;
  /** The value for the option */
  value: string;
  /** Flag indicating if the option is disabled */
  isDisabled?: boolean;
  /** Internal flag indicating if the option is checked */
  isChecked?: boolean;
  /** Internal callback for ref tracking */
  sendRef?: (ref: React.ReactNode, index: number) => void;
  /** Internal callback for keyboard navigation */
  keyHandler?: (index: number, position: string) => void;
  /** Optional callback for click event */
  onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
}

export class CheckboxSelectOption extends React.Component<CheckboxSelectOptionProps> {
  private ref = React.createRef<any>();
  static defaultProps: CheckboxSelectOptionProps = {
    className: '',
    value: '',
    index: 0,
    isDisabled: false,
    isChecked: false,
    onClick: () => {},
    sendRef: () => {},
    keyHandler: () => {}
  };

  componentDidMount() {
    this.props.sendRef(this.ref.current, this.props.index);
  }

  componentDidUpdate() {
    this.props.sendRef(this.ref.current, this.props.index);
  }

  onKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === KeyTypes.Tab) {
      return;
    }
    event.preventDefault();
    if (event.key === KeyTypes.ArrowUp) {
      this.props.keyHandler(this.props.index, 'up');
    } else if (event.key === KeyTypes.ArrowDown) {
      this.props.keyHandler(this.props.index, 'down');
    } else if (event.key === KeyTypes.Enter) {
      this.ref.current.click();
      this.ref.current.focus();
    }
  };

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const {
      children,
      className,
      value,
      onClick,
      isDisabled,
      isChecked,
      sendRef,
      keyHandler,
      index,
      ...props
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */
    return (
      <SelectConsumer>
        {({ onSelect }) => (
          <label
            {...props}
            className={css(
              checkStyles.check,
              styles.selectMenuItem,
              isDisabled && styles.modifiers.disabled,
              className
            )}
            onKeyDown={this.onKeyDown}
          >
            <input
              id={value}
              className={css(checkStyles.checkInput)}
              type="checkbox"
              onChange={event => {
                if (!isDisabled) {
                  onClick(event);
                  onSelect && onSelect(event, value);
                }
              }}
              ref={this.ref}
              checked={isChecked || false}
              disabled={isDisabled}
            />
            <span className={css(checkStyles.checkLabel, isDisabled && styles.modifiers.disabled)}>
              {children || value}
            </span>
          </label>
        )}
      </SelectConsumer>
    );
  }
}
