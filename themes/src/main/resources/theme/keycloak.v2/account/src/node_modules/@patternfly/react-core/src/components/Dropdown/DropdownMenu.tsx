import * as React from 'react';
import * as ReactDOM from 'react-dom';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
import { keyHandler, formatBreakpointMods } from '../../helpers/util';
import { DropdownPosition, DropdownArrowContext, DropdownContext } from './dropdownConstants';

export interface DropdownMenuProps {
  /** Anything which can be rendered as dropdown items */
  children?: React.ReactNode;
  /** Classess applied to root element of dropdown menu */
  className?: string;
  /** Flag to indicate if menu is opened */
  isOpen?: boolean;
  /** @deprecated - no longer used */
  openedOnEnter?: boolean;
  /** Flag to indicate if the first dropdown item should gain initial focus, set false when adding
   * a specific auto-focus item (like a current selection) otherwise leave as true
   */
  autoFocus?: boolean;
  /** Indicates which component will be used as dropdown menu */
  component?: React.ReactNode;
  /** Indicates where menu will be alligned horizontally */
  position?: DropdownPosition | 'right' | 'left';
  /** Indicates how the menu will align at screen size breakpoints */
  alignments?: {
    sm?: 'right' | 'left';
    md?: 'right' | 'left';
    lg?: 'right' | 'left';
    xl?: 'right' | 'left';
    '2xl'?: 'right' | 'left';
  };
  /** Flag to indicate if menu is grouped */
  isGrouped?: boolean;
  // Function to call on component mount
  setMenuComponentRef?: any;
}

export interface DropdownMenuItem extends React.HTMLAttributes<any> {
  isDisabled: boolean;
  disabled: boolean;
  isHovered: boolean;
  ref: HTMLElement;
}

export class DropdownMenu extends React.Component<DropdownMenuProps> {
  static displayName = 'DropdownMenu';
  context!: React.ContextType<typeof DropdownContext>;
  refsCollection = [] as HTMLElement[][];

  static defaultProps: DropdownMenuProps = {
    className: '',
    isOpen: true,
    openedOnEnter: false,
    autoFocus: true,
    position: DropdownPosition.left,
    component: 'ul',
    isGrouped: false,
    setMenuComponentRef: null
  };

  componentDidMount() {
    document.addEventListener('keydown', this.onKeyDown);
    const { autoFocus } = this.props;

    if (autoFocus) {
      // Focus first non-disabled element
      const focusTargetCollection = this.refsCollection.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
      const focusTarget = focusTargetCollection && focusTargetCollection[0];
      if (focusTarget && focusTarget.focus) {
        setTimeout(() => focusTarget.focus());
      }
    }
  }

  componentWillUnmount = () => {
    document.removeEventListener('keydown', this.onKeyDown);
  };

  static validToggleClasses = [styles.dropdownToggle, styles.dropdownToggleButton] as string[];
  static focusFirstRef = (refCollection: HTMLElement[]) => {
    if (refCollection && refCollection[0] && refCollection[0].focus) {
      setTimeout(() => refCollection[0].focus());
    }
  };

  onKeyDown = (event: any) => {
    if (
      !this.props.isOpen ||
      !Array.from(document.activeElement.classList).find(className =>
        DropdownMenu.validToggleClasses.concat(this.context.toggleClass).includes(className)
      )
    ) {
      return;
    }
    const refs = this.refsCollection;
    if (event.key === 'ArrowDown') {
      const firstFocusTargetCollection = refs.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
      DropdownMenu.focusFirstRef(firstFocusTargetCollection);
      event.stopPropagation();
    } else if (event.key === 'ArrowUp') {
      const collectionLength = refs.length;
      const lastFocusTargetCollection = refs.slice(collectionLength - 1, collectionLength);
      const lastFocusTarget = lastFocusTargetCollection && lastFocusTargetCollection[0];
      DropdownMenu.focusFirstRef(lastFocusTarget);
      event.stopPropagation();
    }
  };

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
      } else if (isSeparator) {
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
        const group = groupedChildren as React.ReactElement;
        const props: { children?: React.ReactNode } = {};
        if (group.props && group.props.children) {
          if (Array.isArray(group.props.children)) {
            props.children = React.Children.map(group.props.children, option =>
              React.cloneElement(option as React.ReactElement, {
                index: index++
              })
            );
          } else {
            props.children = React.cloneElement(group.props.children as React.ReactElement, {
              index: index++
            });
          }
        }
        return React.cloneElement(group, props);
      });
    }
    return React.Children.map(children, (child, index) =>
      React.cloneElement(child as React.ReactElement, {
        index
      })
    );
  }

  render() {
    const {
      className,
      isOpen,
      position,
      children,
      component,
      isGrouped,
      setMenuComponentRef,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      openedOnEnter,
      alignments,
      ...props
    } = this.props;
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
                  formatBreakpointMods(alignments, styles, 'align-'),
                  className
                )}
                hidden={!isOpen}
                onClick={event => onSelect && onSelect(event)}
                ref={setMenuComponentRef}
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
                      formatBreakpointMods(alignments, styles, 'align-'),
                      className
                    )}
                    hidden={!isOpen}
                    role="menu"
                    ref={setMenuComponentRef}
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
                      formatBreakpointMods(alignments, styles, 'align-'),
                      className
                    )}
                    hidden={!isOpen}
                    role="menu"
                    ref={setMenuComponentRef}
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

DropdownMenu.contextType = DropdownContext;
