import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import { ContextSelectorToggle } from './ContextSelectorToggle';
import { ContextSelectorMenuList } from './ContextSelectorMenuList';
import { ContextSelectorContext } from './contextSelectorConstants';
import { Button, ButtonVariant } from '../Button';
import { TextInput } from '../TextInput';
import { InputGroup } from '../InputGroup';
import { KEY_CODES } from '../../helpers/constants';
import { FocusTrap } from '../../helpers';
import { ToggleMenuBaseProps } from '../../helpers/Popper/Popper';
import { Popper } from '../../helpers/Popper/Popper';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';

// seed for the aria-labelledby ID
let currentId = 0;
const newId = currentId++;

export interface ContextSelectorProps extends Omit<ToggleMenuBaseProps, 'menuAppendTo'>, OUIAProps {
  /** content rendered inside the Context Selector */
  children?: React.ReactNode;
  /** Classes applied to root element of Context Selector */
  className?: string;
  /** Flag to indicate if Context Selector is opened */
  isOpen?: boolean;
  /** Function callback called when user clicks toggle button */
  onToggle?: (event: any, value: boolean) => void;
  /** Function callback called when user selects item */
  onSelect?: (event: any, value: React.ReactNode) => void;
  /** The container to append the menu to. Defaults to 'inline'.
   * If your menu is being cut off you can append it to an element higher up the DOM tree.
   * Some examples:
   * menuAppendTo="parent"
   * menuAppendTo={() => document.body}
   * menuAppendTo={document.getElementById('target')}
   */
  menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
  /** Flag indicating that the context selector should expand to full height */
  isFullHeight?: boolean;
  /** Labels the Context Selector for Screen Readers */
  screenReaderLabel?: string;
  /** Text that appears in the Context Selector Toggle */
  toggleText?: string;
  /** Aria-label for the Context Selector Search Button */
  searchButtonAriaLabel?: string;
  /** Value in the Search field */
  searchInputValue?: string;
  /** Function callback called when user changes the Search Input */
  onSearchInputChange?: (value: string) => void;
  /** Search Input placeholder */
  searchInputPlaceholder?: string;
  /** Function callback for when Search Button is clicked */
  onSearchButtonClick?: (event?: React.SyntheticEvent<HTMLButtonElement>) => void;
  /** Footer of the context selector */
  footer?: React.ReactNode;
  /** Flag to indicate the toggle has no border or background */
  isPlain?: boolean;
  /** Flag to indicate if toggle is textual toggle */
  isText?: boolean;
  /** Flag to disable focus trap */
  disableFocusTrap?: boolean;
  /** Flag for indicating that the context selector menu should automatically flip vertically when
   * it reaches the boundary. This prop can only be used when the context selector component is not
   * appended inline, e.g. `menuAppendTo="parent"`
   */
  isFlipEnabled?: boolean;
}

export class ContextSelector extends React.Component<ContextSelectorProps, { ouiaStateId: string }> {
  static displayName = 'ContextSelector';
  static defaultProps: ContextSelectorProps = {
    children: null as React.ReactNode,
    className: '',
    isOpen: false,
    onToggle: () => undefined as any,
    onSelect: () => undefined as any,
    screenReaderLabel: '',
    toggleText: '',
    searchButtonAriaLabel: 'Search menu items',
    searchInputValue: '',
    onSearchInputChange: () => undefined as any,
    searchInputPlaceholder: 'Search',
    onSearchButtonClick: () => undefined as any,
    menuAppendTo: 'inline',
    ouiaSafe: true,
    disableFocusTrap: false,
    footer: null as React.ReactNode,
    isPlain: false,
    isText: false,
    isFlipEnabled: false
  };
  constructor(props: ContextSelectorProps) {
    super(props);
    this.state = {
      ouiaStateId: getDefaultOUIAId(ContextSelector.displayName)
    };
  }

  parentRef: React.RefObject<HTMLDivElement> = React.createRef();
  popperRef: React.RefObject<HTMLDivElement> = React.createRef();

  onEnterPressed = (event: any) => {
    if (event.charCode === KEY_CODES.ENTER) {
      this.props.onSearchButtonClick();
    }
  };

  render() {
    const toggleId = `pf-context-selector-toggle-id-${newId}`;
    const screenReaderLabelId = `pf-context-selector-label-id-${newId}`;
    const searchButtonId = `pf-context-selector-search-button-id-${newId}`;
    const {
      children,
      className,
      isOpen,
      isFullHeight,
      onToggle,
      onSelect,
      screenReaderLabel,
      toggleText,
      searchButtonAriaLabel,
      searchInputValue,
      onSearchInputChange,
      searchInputPlaceholder,
      onSearchButtonClick,
      menuAppendTo,
      ouiaId,
      ouiaSafe,
      isPlain,
      isText,
      footer,
      disableFocusTrap,
      isFlipEnabled,
      ...props
    } = this.props;
    const menuContainer = (
      <div
        className={css(styles.contextSelectorMenu)}
        // This removes the `position: absolute`styling from the `.pf-c-context-selector__menu`
        // allowing the menu to flip correctly
        {...(isFlipEnabled && { style: { position: 'revert' } })}
      >
        {isOpen && (
          <FocusTrap
            active={!disableFocusTrap}
            focusTrapOptions={{ clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }}
          >
            <div className={css(styles.contextSelectorMenuSearch)}>
              <InputGroup>
                <TextInput
                  value={searchInputValue}
                  type="search"
                  placeholder={searchInputPlaceholder}
                  onChange={onSearchInputChange}
                  onKeyPress={this.onEnterPressed}
                  aria-labelledby={searchButtonId}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label={searchButtonAriaLabel}
                  id={searchButtonId}
                  onClick={onSearchButtonClick}
                >
                  <SearchIcon aria-hidden="true" />
                </Button>
              </InputGroup>
            </div>
            <ContextSelectorContext.Provider value={{ onSelect }}>
              <ContextSelectorMenuList isOpen={isOpen}>{children}</ContextSelectorMenuList>
            </ContextSelectorContext.Provider>
            {footer}
          </FocusTrap>
        )}
      </div>
    );
    const popperContainer = (
      <div
        className={css(styles.contextSelector, isOpen && styles.modifiers.expanded, className)}
        ref={this.popperRef}
        {...props}
      >
        {isOpen && menuContainer}
      </div>
    );
    const mainContainer = (
      <div
        className={css(
          styles.contextSelector,
          isOpen && styles.modifiers.expanded,
          isFullHeight && styles.modifiers.fullHeight,
          className
        )}
        ref={this.parentRef}
        {...getOUIAProps(ContextSelector.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
        {...props}
      >
        {screenReaderLabel && (
          <span id={screenReaderLabelId} hidden>
            {screenReaderLabel}
          </span>
        )}
        <ContextSelectorToggle
          onToggle={onToggle}
          isOpen={isOpen}
          toggleText={toggleText}
          id={toggleId}
          parentRef={menuAppendTo === 'inline' ? this.parentRef : this.popperRef}
          aria-labelledby={`${screenReaderLabelId} ${toggleId}`}
          isPlain={isPlain}
          isText={isText}
        />
        {isOpen && menuAppendTo === 'inline' && menuContainer}
      </div>
    );
    const getParentElement = () => {
      if (this.parentRef && this.parentRef.current) {
        return this.parentRef.current.parentElement;
      }
      return null;
    };
    return menuAppendTo === 'inline' ? (
      mainContainer
    ) : (
      <Popper
        trigger={mainContainer}
        popper={popperContainer}
        appendTo={menuAppendTo === 'parent' ? getParentElement() : menuAppendTo}
        isVisible={isOpen}
      />
    );
  }
}
