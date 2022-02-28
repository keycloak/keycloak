import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';
import { KEYHANDLER_DIRECTION } from '../../helpers/constants';
import { Tooltip } from '../Tooltip';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';

export interface InternalDropdownItemProps extends React.HTMLProps<HTMLAnchorElement> {
  /** Anything which can be rendered as dropdown item */
  children?: React.ReactNode;
  /** Classes applied to root element of dropdown item */
  className?: string;
  /** Class applied to list element */
  listItemClassName?: string;
  /** Indicates which component will be used as dropdown item */
  component?: React.ReactNode;
  /** Variant of the item. The 'icon' variant should use DropdownItemIcon to wrap contained icons or images. */
  variant?: 'item' | 'icon';
  /** Role for the item */
  role?: string;
  /** Render dropdown item as disabled option */
  isDisabled?: boolean;
  /** Forces display of the hover state of the element */
  isHovered?: boolean;
  /** Default hyperlink location */
  href?: string;
  /** Tooltip to display when hovered over the item */
  tooltip?: React.ReactNode;
  /** Additional tooltip props forwarded to the Tooltip component */
  tooltipProps?: any;
  index?: number;
  context?: {
    keyHandler?: (index: number, innerIndex: number, direction: string) => void;
    sendRef?: (index: number, ref: any, isDisabled: boolean, isSeparator: boolean) => void;
  };
  /** Callback for click event */
  onClick?: (event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => void;
  /** ID for the list element */
  id?: string;
  /** ID for the component element */
  componentID?: string;
  /** Additional content to include alongside item within the <li> */
  additionalChild?: React.ReactNode;
  /** Custom item rendering that receives the DropdownContext */
  customChild?: React.ReactNode;
  /** Flag indicating if hitting enter on an item also triggers an arrow down key press */
  enterTriggersArrowDown?: boolean;
}

export class InternalDropdownItem extends React.Component<InternalDropdownItemProps> {
  ref = React.createRef<HTMLLIElement>();
  additionalRef = React.createRef<any>();

  static defaultProps: InternalDropdownItemProps = {
    className: '',
    isHovered: false,
    component: 'a',
    variant: 'item',
    role: 'none',
    isDisabled: false,
    tooltipProps: {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => undefined as any,
    index: -1,
    context: {
      keyHandler: () => {},
      sendRef: () => {}
    },
    enterTriggersArrowDown: false
  };

  componentDidMount() {
    const { context, index, isDisabled, role, customChild } = this.props;
    const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
    context.sendRef(
      index,
      [customRef, customChild ? customRef : this.additionalRef.current],
      isDisabled,
      role === 'separator'
    );
  }

  componentDidUpdate() {
    const { context, index, isDisabled, role, customChild } = this.props;
    const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
    context.sendRef(
      index,
      [customRef, customChild ? customRef : this.additionalRef.current],
      isDisabled,
      role === 'separator'
    );
  }

  getInnerNode = (node: any) => (node && node.childNodes && node.childNodes.length ? node.childNodes[0] : node);

  onKeyDown = (event: any) => {
    // Detected key press on this item, notify the menu parent so that the appropriate item can be focused
    const innerIndex = event.target === this.ref.current ? 0 : 1;
    if (!this.props.customChild) {
      event.preventDefault();
    }
    if (event.key === 'ArrowUp') {
      this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.UP);
    } else if (event.key === 'ArrowDown') {
      this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
    } else if (event.key === 'ArrowRight') {
      this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.RIGHT);
    } else if (event.key === 'ArrowLeft') {
      this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.LEFT);
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.target.click();
      this.props.enterTriggersArrowDown &&
        this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
    }
  };

  extendAdditionalChildRef() {
    const { additionalChild } = this.props;

    return React.cloneElement(additionalChild as React.ReactElement<any>, {
      ref: this.additionalRef
    });
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const {
      className,
      children,
      isHovered,
      context,
      onClick,
      component,
      variant,
      role,
      isDisabled,
      index,
      href,
      tooltip,
      tooltipProps,
      id,
      componentID,
      listItemClassName,
      additionalChild,
      customChild,
      enterTriggersArrowDown,
      ...additionalProps
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */
    const Component = component as any;
    let classes: string;

    if (Component === 'a') {
      additionalProps['aria-disabled'] = isDisabled;
      additionalProps.tabIndex = isDisabled ? -1 : additionalProps.tabIndex;
    } else if (Component === 'button') {
      additionalProps.disabled = isDisabled;
      additionalProps.type = additionalProps.type || 'button';
    }

    const renderWithTooltip = (childNode: React.ReactNode) =>
      tooltip ? (
        <Tooltip content={tooltip} {...tooltipProps}>
          {childNode}
        </Tooltip>
      ) : (
        childNode
      );

    return (
      <DropdownContext.Consumer>
        {({ onSelect, itemClass, disabledClass, hoverClass }) => {
          if (this.props.role === 'separator') {
            classes = css(variant === 'icon' && styles.modifiers.icon, className);
          } else {
            classes = css(
              variant === 'icon' && styles.modifiers.icon,
              className,
              isDisabled && disabledClass,
              isHovered && hoverClass,
              itemClass
            );
          }
          if (customChild) {
            return React.cloneElement(customChild as React.ReactElement<any>, {
              ref: this.ref,
              onKeyDown: this.onKeyDown
            });
          }
          return (
            <li
              className={listItemClassName || null}
              role={role}
              onKeyDown={this.onKeyDown}
              onClick={(event: any) => {
                if (!isDisabled) {
                  onClick(event);
                  onSelect(event);
                }
              }}
              id={id}
            >
              {renderWithTooltip(
                React.isValidElement(component) ? (
                  React.cloneElement(component as React.ReactElement<any>, {
                    href,
                    id: componentID,
                    className: classes,
                    ...additionalProps
                  })
                ) : (
                  <Component {...additionalProps} href={href} ref={this.ref} className={classes} id={componentID}>
                    {children}
                  </Component>
                )
              )}
              {additionalChild && this.extendAdditionalChildRef()}
            </li>
          );
        }}
      </DropdownContext.Consumer>
    );
  }
}
