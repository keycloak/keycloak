import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { InjectedOuiaProps, withOuiaContext } from '../withOuia';

export type NavSelectClickHandler = (
  e: React.FormEvent<HTMLInputElement>,
  itemId: number | string,
  groupId: number | string,
  to: string
) => void;

export interface NavProps
  extends Omit<React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>, 'onSelect'> {
  /** Anything that can be rendered inside of the nav */
  children?: React.ReactNode;
  /** Additional classes added to the container */
  className?: string;
  /** Callback for updating when item selection changes */
  onSelect?: (selectedItem: {
    groupId: number | string;
    itemId: number | string;
    to: string;
    event: React.FormEvent<HTMLInputElement>;
  }) => void;
  /** Callback for when a list is expanded or collapsed */
  onToggle?: (toggledItem: {
    groupId: number | string;
    isExpanded: boolean;
    event: React.FormEvent<HTMLInputElement>;
  }) => void;
  /** Accessibility label */
  'aria-label'?: string;
  /** Indicates which theme color to use */
  theme?: 'dark' | 'light';
}

export const NavContext = React.createContext({});

class Nav extends React.Component<NavProps & InjectedOuiaProps> {
  static defaultProps: NavProps = {
    'aria-label': '',
    children: null,
    className: '',
    onSelect: () => undefined,
    onToggle: () => undefined,
    theme: 'light'
  };

  state = {
    showLeftScrollButton: false,
    showRightScrollButton: false
  };

  updateScrollButtonState = (state: { showLeftScrollButton: boolean; showRightScrollButton: boolean }) => {
    const { showLeftScrollButton, showRightScrollButton } = state;
    this.setState({
      showLeftScrollButton,
      showRightScrollButton
    });
  };

  // Callback from NavItem
  onSelect(
    event: React.FormEvent<HTMLInputElement>,
    groupId: number | string,
    itemId: number | string,
    to: string,
    preventDefault: boolean,
    onClick: NavSelectClickHandler
  ) {
    if (preventDefault) {
      event.preventDefault();
    }

    this.props.onSelect({ groupId, itemId, event, to });

    if (onClick) {
      onClick(event, itemId, groupId, to);
    }
  }

  // Callback from NavExpandable
  onToggle(event: React.MouseEvent<HTMLInputElement>, groupId: number | string, toggleValue: boolean) {
    this.props.onToggle({
      event,
      groupId,
      isExpanded: toggleValue
    });
  }

  render() {
    const {
      'aria-label': ariaLabel,
      children,
      className,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onSelect,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onToggle,
      theme,
      ouiaContext,
      ouiaId,
      ...props
    } = this.props;
    const { showLeftScrollButton, showRightScrollButton } = this.state;
    const childrenProps: any = (children as any).props;

    return (
      <NavContext.Provider
        value={{
          onSelect: (
            event: React.FormEvent<HTMLInputElement>,
            groupId: number | string,
            itemId: number | string,
            to: string,
            preventDefault: boolean,
            onClick: (
              e: React.FormEvent<HTMLInputElement>,
              itemId: number | string,
              groupId: number | string,
              to: string
            ) => void
          ) => this.onSelect(event, groupId, itemId, to, preventDefault, onClick),
          onToggle: (event: React.MouseEvent<HTMLInputElement>, groupId: number | string, expanded: boolean) =>
            this.onToggle(event, groupId, expanded),
          updateScrollButtonState: this.updateScrollButtonState
        }}
      >
        <nav
          className={css(
            styles.nav,
            theme === 'dark' && styles.modifiers.dark,
            showLeftScrollButton && styles.modifiers.start,
            showRightScrollButton && styles.modifiers.end,
            className
          )}
          aria-label={
            ariaLabel === ''
              ? typeof childrenProps !== 'undefined' && childrenProps.variant === 'tertiary'
                ? 'Local'
                : 'Global'
              : ariaLabel
          }
          {...(ouiaContext.isOuia && {
            'data-ouia-component-type': 'Nav',
            'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
          })}
          {...props}
        >
          {children}
        </nav>
      </NavContext.Provider>
    );
  }
}

const NavWithOuiaContext = withOuiaContext(Nav);
export { NavWithOuiaContext as Nav };
