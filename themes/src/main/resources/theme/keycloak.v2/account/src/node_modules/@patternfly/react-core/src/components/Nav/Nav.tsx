import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';

export type NavSelectClickHandler = (
  e: React.FormEvent<HTMLInputElement>,
  itemId: number | string,
  groupId: number | string,
  to: string
) => void;

export interface NavProps
  extends Omit<React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>, 'onSelect'>,
    OUIAProps {
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
    event: React.MouseEvent<HTMLButtonElement>;
  }) => void;
  /** Accessibility label */
  'aria-label'?: string;
  /** Indicates which theme color to use */
  theme?: 'dark' | 'light';
  /** For horizontal navs */
  variant?: 'default' | 'horizontal' | 'tertiary' | 'horizontal-subnav';
}

export interface NavContextProps {
  onSelect?: (
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
  ) => void;
  onToggle?: (event: React.MouseEvent<HTMLButtonElement>, groupId: number | string, expanded: boolean) => void;
  updateIsScrollable?: (isScrollable: boolean) => void;
  isHorizontal?: boolean;
  flyoutRef?: React.Ref<HTMLLIElement>;
  setFlyoutRef?: (ref: React.Ref<HTMLLIElement>) => void;
}
export const navContextDefaults = {};
export const NavContext = React.createContext<NavContextProps>(navContextDefaults);

export class Nav extends React.Component<
  NavProps,
  { isScrollable: boolean; ouiaStateId: string; flyoutRef: React.Ref<HTMLLIElement> | null }
> {
  static displayName = 'Nav';
  static defaultProps: NavProps = {
    onSelect: () => undefined,
    onToggle: () => undefined,
    theme: 'dark',
    ouiaSafe: true
  };

  state = {
    isScrollable: false,
    ouiaStateId: getDefaultOUIAId(Nav.displayName, this.props.variant),
    flyoutRef: null as React.Ref<HTMLLIElement>
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
  onToggle(event: React.MouseEvent<HTMLButtonElement>, groupId: number | string, toggleValue: boolean) {
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
      ouiaId,
      ouiaSafe,
      variant,
      ...props
    } = this.props;
    const isHorizontal = ['horizontal', 'tertiary'].includes(variant);

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
          onToggle: (event: React.MouseEvent<HTMLButtonElement>, groupId: number | string, expanded: boolean) =>
            this.onToggle(event, groupId, expanded),
          updateIsScrollable: (isScrollable: boolean) => this.setState({ isScrollable }),
          isHorizontal: ['horizontal', 'tertiary', 'horizontal-subnav'].includes(variant),
          flyoutRef: this.state.flyoutRef,
          setFlyoutRef: flyoutRef => this.setState({ flyoutRef })
        }}
      >
        <nav
          className={css(
            styles.nav,
            theme === 'light' && styles.modifiers.light,
            isHorizontal && styles.modifiers.horizontal,
            variant === 'tertiary' && styles.modifiers.tertiary,
            variant === 'horizontal-subnav' && styles.modifiers.horizontalSubnav,
            this.state.isScrollable && styles.modifiers.scrollable,
            className
          )}
          aria-label={ariaLabel || (variant === 'tertiary' ? 'Local' : 'Global')}
          {...getOUIAProps(Nav.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
          {...props}
        >
          {children}
        </nav>
      </NavContext.Provider>
    );
  }
}
