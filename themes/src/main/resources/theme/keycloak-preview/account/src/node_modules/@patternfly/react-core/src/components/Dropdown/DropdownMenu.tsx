import * as React from 'react';
import * as ReactDOM from 'react-dom';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
import { keyHandler } from '../../helpers/util';
import { DropdownPosition, DropdownArrowContext, DropdownContext } from './dropdownConstants';

export interface DropdownMenuProps {
  /** Anything which can be rendered as dropdown items */
  children?: React.ReactNode;
  /** Classess applied to root element of dropdown menu */
  className?: string;
  /** Flag to indicate if menu is opened */
  isOpen?: boolean;
  /** Flag to indicate if menu should be opened on enter */
  openedOnEnter?: boolean;
  /** Flag to indicate if the first dropdown item should gain initial focus, set false when adding
   * a specific auto-focus item (like a current selection) otherwise leave as true
   */
  autoFocus?: boolean;
  /** Indicates which component will be used as dropdown menu */
  component?: React.ReactNode;
  /** Indicates where menu will be alligned horizontally */
  position?: DropdownPosition | 'right' | 'left';
  /** Flag to indicate if menu is grouped */
  isGrouped?: boolean;
}

export interface DropdownMenuItem extends React.HTMLAttributes<any> {
  isDisabled: boolean;
  disabled: boolean;
  isHovered: boolean;
  ref: HTMLElement;
}

export class DropdownMenu extends React.Component<DropdownMenuProps> {
  refsCollection = [] as HTMLElement[][];

  static defaultProps: DropdownMenuProps = {
    className: '',
    isOpen: true,
    openedOnEnter: false,
    autoFocus: true,
    position: DropdownPosition.left,
    component: 'ul',
    isGrouped: false
  };

  componentDidMount() {
    const { autoFocus } = this.props;

    if (autoFocus) {
      // Focus first non-disabled element
      const focusTargetCollection = this.refsCollection.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
      const focusTarget = focusTargetCollection && focusTargetCollection[0];
      if (focusTarget && focusTarget.focus) {
        focusTarget.focus();
      }
    }
  }

  shouldComponentUpdate() {
    // reset refsCollection before updating to account for child removal between mounts
    this.refsCollection = [] as HTMLElement[][];
    return true;
  }

  childKeyHandler = (index: number, innerIndex: number, position: string, custom = false) => {
    keyHandler(
      index,
      innerIndex,
      position,
      this.refsCollection,
      this.props.isGrouped ? this.refsCollection : React.Children.toArray(this.props.children),
      custom
    );
  };

  sendRef = (index: number, nodes: any[], isDisabled: boolean, isSeparator: boolean) => {
    this.refsCollection[index] = [];
    nodes.map((node, innerIndex) => {
      if (!node) {
        this.refsCollection[index][innerIndex] = null;
      } else if (!node.getAttribute) {
        // eslint-disable-next-line react/no-find-dom-node
        this.refsCollection[index][innerIndex] = ReactDOM.findDOMNode(node) as HTMLElement;
      } else if (isDisabled || isSeparator) {
        this.refsCollection[index][innerIndex] = null;
      } else {
        this.refsCollection[index][innerIndex] = node;
      }
    });
  };

  extendChildren() {
    const { children, isGrouped } = this.props;
    if (isGrouped) {
      let index = 0;
      return React.Children.map(children, groupedChildren => {
        const group = groupedChildren as React.ReactElement<{ children: React.ReactNode }>;
        return React.cloneElement(group, {
          ...(group.props &&
            group.props.children && {
              children:
                (group.props.children.constructor === Array &&
                  React.Children.map(
                    group.props.children as React.ReactElement<any>,
                    (option: React.ReactElement<any>) =>
                      React.cloneElement(option, {
                        index: index++
                      })
                  )) ||
                React.cloneElement(group.props.children as React.ReactElement<any>, {
                  index: index++
                })
            })
        });
      });
    }
    return React.Children.map(children, (child, index) =>
      React.cloneElement(child as React.ReactElement<any>, {
        index
      })
    );
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { className, isOpen, position, children, component, isGrouped, openedOnEnter, ...props } = this.props;
    return (
      <DropdownArrowContext.Provider
        value={{
          keyHandler: this.childKeyHandler,
          sendRef: this.sendRef
        }}
      >
        {component === 'div' ? (
          <DropdownContext.Consumer>
            {({ onSelect, menuClass }) => (
              <div
                className={css(
                  menuClass,
                  position === DropdownPosition.right && styles.modifiers.alignRight,
                  className
                )}
                hidden={!isOpen}
                onClick={event => onSelect && onSelect(event)}
              >
                {children}
              </div>
            )}
          </DropdownContext.Consumer>
        ) : (
          (isGrouped && (
            <DropdownContext.Consumer>
              {({ menuClass, menuComponent }) => {
                const MenuComponent = (menuComponent || 'div') as any;
                return (
                  <MenuComponent
                    {...props}
                    className={css(
                      menuClass,
                      position === DropdownPosition.right && styles.modifiers.alignRight,
                      className
                    )}
                    hidden={!isOpen}
                    role="menu"
                  >
                    {this.extendChildren()}
                  </MenuComponent>
                );
              }}
            </DropdownContext.Consumer>
          )) || (
            <DropdownContext.Consumer>
              {({ menuClass, menuComponent }) => {
                const MenuComponent = (menuComponent || component) as any;
                return (
                  <MenuComponent
                    {...props}
                    className={css(
                      menuClass,
                      position === DropdownPosition.right && styles.modifiers.alignRight,
                      className
                    )}
                    hidden={!isOpen}
                    role="menu"
                  >
                    {this.extendChildren()}
                  </MenuComponent>
                );
              }}
            </DropdownContext.Consumer>
          )
        )}
      </DropdownArrowContext.Provider>
    );
  }
}
