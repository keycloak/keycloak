import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
import { DropdownMenu } from './DropdownMenu';
import { DropdownProps } from './Dropdown';
import { DropdownContext, DropdownDirection, DropdownPosition } from './dropdownConstants';
import { getOUIAProps, OUIAProps } from '../../helpers';
import { PickOptional } from '../../helpers/typeUtils';
import { Popper } from '../../helpers/Popper/Popper';

export class DropdownWithContext extends React.Component<DropdownProps & OUIAProps> {
  static displayName = 'DropdownWithContext';

  openedOnEnter = false;
  baseComponentRef = React.createRef<any>();
  menuComponentRef = React.createRef<any>();

  // seed for the aria-labelledby ID
  static currentId = 0;

  static defaultProps: PickOptional<DropdownProps> = {
    className: '',
    dropdownItems: [] as any[],
    isOpen: false,
    isPlain: false,
    isText: false,
    isGrouped: false,
    position: DropdownPosition.left,
    direction: DropdownDirection.down,
    onSelect: (): void => undefined,
    autoFocus: true,
    menuAppendTo: 'inline',
    isFlipEnabled: false
  };

  constructor(props: DropdownProps & OUIAProps) {
    super(props);
    if (props.dropdownItems && props.dropdownItems.length > 0 && props.children) {
      // eslint-disable-next-line no-console
      console.error(
        'Children and dropdownItems props have been provided. Only the dropdownItems prop items will be rendered'
      );
    }
  }

  onEnter = () => {
    this.openedOnEnter = true;
  };

  componentDidUpdate() {
    if (!this.props.isOpen) {
      this.openedOnEnter = false;
    }
  }

  setMenuComponentRef = (element: any) => {
    this.menuComponentRef = element;
  };

  getMenuComponentRef = () => this.menuComponentRef;

  render() {
    const {
      children,
      className,
      direction,
      dropdownItems,
      isOpen,
      isPlain,
      isText,
      isGrouped,
      isFullHeight,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onSelect,
      position,
      toggle,
      autoFocus,
      menuAppendTo,
      isFlipEnabled,
      ...props
    } = this.props;
    const id = toggle.props.id || `pf-dropdown-toggle-id-${DropdownWithContext.currentId++}`;
    let component: string;
    let renderedContent: React.ReactNode[];
    let ariaHasPopup = false;
    if (dropdownItems && dropdownItems.length > 0) {
      component = 'ul';
      renderedContent = dropdownItems;
      ariaHasPopup = true;
    } else {
      component = 'div';
      renderedContent = React.Children.toArray(children);
    }
    const openedOnEnter = this.openedOnEnter;
    return (
      <DropdownContext.Consumer>
        {({ baseClass, baseComponent, id: contextId, ouiaId, ouiaComponentType, ouiaSafe, alignments }) => {
          const BaseComponent = baseComponent as any;
          const menuContainer = (
            <DropdownMenu
              // This removes the `position: absolute` styling from the `.pf-c-dropdown__menu`
              // allowing the menu to flip correctly
              {...(isFlipEnabled && { style: { position: 'revert', minWidth: 'min-content' } })}
              setMenuComponentRef={this.setMenuComponentRef}
              component={component}
              isOpen={isOpen}
              position={position}
              aria-labelledby={contextId ? `${contextId}-toggle` : id}
              isGrouped={isGrouped}
              autoFocus={openedOnEnter && autoFocus}
              alignments={alignments}
            >
              {renderedContent}
            </DropdownMenu>
          );
          const popperContainer = (
            <div
              className={css(
                baseClass,
                direction === DropdownDirection.up && styles.modifiers.top,
                position === DropdownPosition.right && styles.modifiers.alignRight,
                isOpen && styles.modifiers.expanded,
                className
              )}
            >
              {isOpen && menuContainer}
            </div>
          );
          const mainContainer = (
            <BaseComponent
              {...props}
              className={css(
                baseClass,
                direction === DropdownDirection.up && styles.modifiers.top,
                position === DropdownPosition.right && styles.modifiers.alignRight,
                isOpen && styles.modifiers.expanded,
                isFullHeight && styles.modifiers.fullHeight,
                className
              )}
              ref={this.baseComponentRef}
              {...getOUIAProps(ouiaComponentType, ouiaId, ouiaSafe)}
            >
              {React.Children.map(toggle, oneToggle =>
                React.cloneElement(oneToggle, {
                  parentRef: this.baseComponentRef,
                  getMenuRef: this.getMenuComponentRef,
                  isOpen,
                  id,
                  isPlain,
                  isText,
                  'aria-haspopup': ariaHasPopup,
                  onEnter: () => {
                    this.onEnter();
                    oneToggle.props.onEnter && oneToggle.props.onEnter();
                  }
                })
              )}
              {menuAppendTo === 'inline' && isOpen && menuContainer}
            </BaseComponent>
          );
          const getParentElement = () => {
            if (this.baseComponentRef && this.baseComponentRef.current) {
              return this.baseComponentRef.current.parentElement;
            }
            return null;
          };
          return menuAppendTo === 'inline' ? (
            mainContainer
          ) : (
            <Popper
              trigger={mainContainer}
              popper={popperContainer}
              direction={direction}
              position={position}
              appendTo={menuAppendTo === 'parent' ? getParentElement() : menuAppendTo}
              isVisible={isOpen}
            />
          );
        }}
      </DropdownContext.Consumer>
    );
  }
}
