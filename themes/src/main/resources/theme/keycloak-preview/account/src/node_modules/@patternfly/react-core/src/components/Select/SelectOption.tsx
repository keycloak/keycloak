import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as checkStyles } from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
import { SelectConsumer, SelectVariant, KeyTypes } from './selectConstants';

export interface SelectOptionObject {
  /** Function returns a string to represent the select option object */
  toString(): string;
  /** Function returns a true if the passed in select option is equal to this select option object, false otherwise */
  compareTo?(selectOption: any): boolean;
}
export interface SelectOptionProps extends Omit<React.HTMLProps<HTMLElement>, 'type' | 'ref' | 'value'> {
  /** Optional alternate display for the option */
  children?: React.ReactNode;
  /** Additional classes added to the Select Option */
  className?: string;
  /** Internal index of the option */
  index?: number;
  /** Indicates which component will be used as select item */
  component?: React.ReactNode;
  /** The value for the option, can be a string or select option object */
  value: string | SelectOptionObject;
  /** Flag indicating if the option is disabled */
  isDisabled?: boolean;
  /** Flag indicating if the option acts as a placeholder */
  isPlaceholder?: boolean;
  /** Flad indicating if the option acts as a "no results" indicator */
  isNoResultsOption?: boolean;
  /** Internal flag indicating if the option is selected */
  isSelected?: boolean;
  /** Internal flag indicating if the option is checked */
  isChecked?: boolean;
  /** Internal flag indicating if the option is focused */
  isFocused?: boolean;
  /** Internal callback for ref tracking */
  sendRef?: (ref: React.ReactNode, index: number) => void;
  /** Internal callback for keyboard navigation */
  keyHandler?: (index: number, position: string) => void;
  /** Optional callback for click event */
  onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
}

export class SelectOption extends React.Component<SelectOptionProps> {
  private ref = React.createRef<any>();
  static defaultProps: SelectOptionProps = {
    className: '',
    value: '',
    index: 0,
    isDisabled: false,
    isPlaceholder: false,
    isSelected: false,
    isChecked: false,
    isFocused: false,
    isNoResultsOption: false,
    component: 'button',
    onClick: () => {},
    sendRef: () => {},
    keyHandler: () => {}
  };

  componentDidMount() {
    this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
  }

  componentDidUpdate() {
    this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
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
      if (this.context.variant === SelectVariant.checkbox) {
        this.ref.current.focus();
      }
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
      isPlaceholder,
      isNoResultsOption,
      isSelected,
      isChecked,
      isFocused,
      sendRef,
      keyHandler,
      index,
      component,
      ...props
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */
    const Component = component as any;
    return (
      <SelectConsumer>
        {({ onSelect, onClose, variant }) => (
          <React.Fragment>
            {variant !== SelectVariant.checkbox && (
              <li role="presentation">
                <Component
                  {...props}
                  className={css(
                    styles.selectMenuItem,
                    isSelected && styles.modifiers.selected,
                    isDisabled && styles.modifiers.disabled,
                    isFocused && styles.modifiers.focus,
                    className
                  )}
                  onClick={(event: any) => {
                    if (!isDisabled) {
                      onClick(event);
                      onSelect(event, value, isPlaceholder);
                      onClose();
                    }
                  }}
                  role="option"
                  aria-selected={isSelected || null}
                  ref={this.ref}
                  onKeyDown={this.onKeyDown}
                  type="button"
                >
                  {children || value.toString()}
                  {isSelected && <CheckIcon className={css(styles.selectMenuItemIcon)} aria-hidden />}
                </Component>
              </li>
            )}
            {variant === SelectVariant.checkbox && !isNoResultsOption && (
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
                  id={value.toString()}
                  className={css(checkStyles.checkInput)}
                  type="checkbox"
                  onChange={event => {
                    if (!isDisabled) {
                      onClick(event);
                      onSelect(event, value);
                    }
                  }}
                  ref={this.ref}
                  checked={isChecked || false}
                  disabled={isDisabled}
                />
                <span className={css(checkStyles.checkLabel, isDisabled && styles.modifiers.disabled)}>
                  {children || value.toString()}
                </span>
              </label>
            )}
            {variant === SelectVariant.checkbox && isNoResultsOption && (
              <div>
                <Component
                  {...props}
                  className={css(
                    styles.selectMenuItem,
                    isSelected && styles.modifiers.selected,
                    isDisabled && styles.modifiers.disabled,
                    isFocused && styles.modifiers.focus,
                    className
                  )}
                  role="option"
                  aria-selected={isSelected || null}
                  ref={this.ref}
                  onKeyDown={this.onKeyDown}
                  type="button"
                >
                  {children || value.toString()}
                </Component>
              </div>
            )}
          </React.Fragment>
        )}
      </SelectConsumer>
    );
  }
}
