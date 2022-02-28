import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as formStyles } from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { SelectOptionObject, SelectOption } from './SelectOption';
import { SelectConsumer, SelectVariant } from './selectConstants';
import { PickOptional } from '../../helpers/typeUtils';

import { FocusTrap } from '../../helpers';

export interface SelectMenuProps extends Omit<React.HTMLProps<HTMLElement>, 'checked' | 'selected' | 'ref'> {
  /** Content rendered inside the SelectMenu */
  children: React.ReactElement[] | React.ReactNode;
  /** Flag indicating that the children is custom content to render inside the SelectMenu.  If true, variant prop is ignored. */
  isCustomContent?: boolean;
  /** Additional classes added to the SelectMenu control */
  className?: string;
  /** Flag indicating the Select is expanded */
  isExpanded?: boolean;
  /** Flag indicating the Select options are grouped */
  isGrouped?: boolean;
  /** Currently selected option (for single, typeahead variants) */
  selected?: string | SelectOptionObject | (string | SelectOptionObject)[];
  /** Currently checked options (for checkbox variant) */
  checked?: (string | SelectOptionObject)[];
  /** Internal flag for specifiying how the menu was opened */
  openedOnEnter?: boolean;
  /** Flag to specify the  maximum height of the menu, as a string percentage or number of pixels */
  maxHeight?: string | number;
  /** Inner prop passed from parent */
  noResultsFoundText?: string;
  /** Inner prop passed from parent */
  createText?: string;
  /** Internal callback for ref tracking */
  sendRef?: (ref: React.ReactNode, index: number) => void;
  /** Internal callback for keyboard navigation */
  keyHandler?: (index: number, position: string) => void;
  /** Flag indicating select has an inline text input for filtering */
  hasInlineFilter?: boolean;
}

export class SelectMenu extends React.Component<SelectMenuProps> {
  static defaultProps: PickOptional<SelectMenuProps> = {
    className: '',
    isExpanded: false,
    isGrouped: false,
    openedOnEnter: false,
    selected: '',
    maxHeight: '',
    sendRef: () => {},
    keyHandler: () => {},
    isCustomContent: false,
    hasInlineFilter: false
  };

  extendChildren() {
    const { children, isGrouped } = this.props;
    const childrenArray: React.ReactElement[] = children as React.ReactElement[];
    if (isGrouped) {
      let index = 0;
      return React.Children.map(childrenArray, (group: React.ReactElement) =>
        React.cloneElement(group, {
          titleId: group.props.label.replace(/\W/g, '-'),
          children: group.props.children.map((option: React.ReactElement) => this.cloneOption(option, index++))
        })
      );
    }
    return React.Children.map(childrenArray, (child: React.ReactElement, index: number) =>
      this.cloneOption(child, index)
    );
  }

  cloneOption(child: React.ReactElement, index: number) {
    const { selected, sendRef, keyHandler } = this.props;
    const isSelected =
      selected && selected.constructor === Array
        ? selected && (Array.isArray(selected) && selected.includes(child.props.value))
        : selected === child.props.value;
    return React.cloneElement(child, {
      id: `${child.props.value ? child.props.value.toString() : ''}-${index}`,
      isSelected,
      sendRef,
      keyHandler,
      index
    });
  }

  extendCheckboxChildren(children: React.ReactElement[]) {
    const { isGrouped, checked, sendRef, keyHandler, hasInlineFilter } = this.props;
    let index = hasInlineFilter ? 1 : 0;
    if (isGrouped) {
      return React.Children.map(children, (group: React.ReactElement) => {
        if (group.type === SelectOption) {
          return group;
        }
        return React.cloneElement(group, {
          titleId: group.props.label.replace(/\W/g, '-'),
          children: (
            <fieldset
              aria-labelledby={group.props.label.replace(/\W/g, '-')}
              className={css(styles.selectMenuFieldset)}
            >
              {group.props.children.map((option: React.ReactElement) =>
                React.cloneElement(option, {
                  isChecked: checked && checked.includes(option.props.value),
                  sendRef,
                  keyHandler,
                  index: index++
                })
              )}
            </fieldset>
          )
        });
      });
    }
    return React.Children.map(children, (child: React.ReactElement) =>
      React.cloneElement(child, {
        isChecked: checked && checked.includes(child.props.value),
        sendRef,
        keyHandler,
        index: index++
      })
    );
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const {
      children,
      isCustomContent,
      className,
      isExpanded,
      openedOnEnter,
      selected,
      checked,
      isGrouped,
      sendRef,
      keyHandler,
      maxHeight,
      noResultsFoundText,
      createText,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      hasInlineFilter,
      ...props
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */
    return (
      <SelectConsumer>
        {({ variant }) => (
          <React.Fragment>
            {isCustomContent && (
              <div
                className={css(styles.selectMenu, className)}
                {...(maxHeight && { style: { maxHeight, overflow: 'auto' } })}
                {...props}
              >
                {children}
              </div>
            )}
            {variant !== SelectVariant.checkbox && !isCustomContent && (
              <ul
                className={css(styles.selectMenu, className)}
                role="listbox"
                {...(maxHeight && { style: { maxHeight, overflow: 'auto' } })}
                {...props}
              >
                {this.extendChildren()}
              </ul>
            )}
            {variant === SelectVariant.checkbox && !isCustomContent && React.Children.count(children) > 0 && (
              <FocusTrap focusTrapOptions={{ clickOutsideDeactivates: true }}>
                <div
                  className={css(styles.selectMenu, className)}
                  {...(maxHeight && { style: { maxHeight, overflow: 'auto' } })}
                >
                  <fieldset
                    {...props}
                    aria-label={ariaLabel}
                    aria-labelledby={(!ariaLabel && ariaLabelledBy) || null}
                    className={css(formStyles.formFieldset)}
                  >
                    {hasInlineFilter && [
                      (children as React.ReactElement[]).shift(),
                      ...this.extendCheckboxChildren(children as React.ReactElement[])
                    ]}
                    {!hasInlineFilter && this.extendCheckboxChildren(children as React.ReactElement[])}
                  </fieldset>
                </div>
              </FocusTrap>
            )}
            {variant === SelectVariant.checkbox && !isCustomContent && React.Children.count(children) === 0 && (
              <div
                className={css(styles.selectMenu, className)}
                {...(maxHeight && { style: { maxHeight, overflow: 'auto' } })}
              >
                <fieldset className={css(styles.selectMenuFieldset)} />
              </div>
            )}
          </React.Fragment>
        )}
      </SelectConsumer>
    );
  }
}
