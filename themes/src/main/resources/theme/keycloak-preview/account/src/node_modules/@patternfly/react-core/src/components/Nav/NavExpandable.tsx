import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import a11yStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { getUniqueId } from '../../helpers/util';
import { NavContext } from './Nav';
import { PickOptional } from '../../helpers/typeUtils';

export interface NavExpandableProps
  extends React.DetailedHTMLProps<React.LiHTMLAttributes<HTMLLIElement>, HTMLLIElement> {
  /** Title shown for the expandable list */
  title: string;
  /** If defined, screen readers will read this text instead of the list title */
  srText?: string;
  /** Boolean to programatically expand or collapse section */
  isExpanded?: boolean;
  /** Anything that can be rendered inside of the expandable list */
  children?: React.ReactNode;
  /** Additional classes added to the container */
  className?: string;
  /** Group identifier, will be returned with the onToggle and onSelect callback passed to the Nav component */
  groupId?: string | number;
  /** If true makes the expandable list title active */
  isActive?: boolean;
  /** Identifier to use for the section aria label */
  id?: string;
  /** allow consumer to optionally override this callback and manage expand state externally */
  onExpand?: (e: React.MouseEvent<HTMLLIElement, MouseEvent>, val: boolean) => void;
}

interface NavExpandableState {
  expandedState: boolean;
}

export class NavExpandable extends React.Component<NavExpandableProps, NavExpandableState> {
  static defaultProps: PickOptional<NavExpandableProps> = {
    srText: '',
    isExpanded: false,
    children: '',
    className: '',
    groupId: null as string,
    isActive: false,
    id: ''
  };

  id = this.props.id || getUniqueId();

  state = {
    expandedState: this.props.isExpanded
  };

  componentDidMount() {
    this.setState({ expandedState: this.props.isExpanded });
  }

  componentDidUpdate(prevProps: NavExpandableProps) {
    if (this.props.isExpanded !== prevProps.isExpanded) {
      this.setState({ expandedState: this.props.isExpanded });
    }
  }

  onExpand = (e: React.MouseEvent<HTMLLIElement, MouseEvent>, val: boolean) => {
    if (this.props.onExpand) {
      this.props.onExpand(e, val);
    } else {
      this.setState({ expandedState: val });
    }
  };

  handleToggle = (
    e: React.MouseEvent<HTMLLIElement, MouseEvent>,
    onToggle: (
      event: React.MouseEvent<HTMLLIElement, MouseEvent>,
      groupId: string | number,
      expandedState: boolean
    ) => void
  ) => {
    // Item events can bubble up, ignore those
    if ((e.target as any).getAttribute('data-component') !== 'pf-nav-expandable') {
      return;
    }

    const { groupId } = this.props;
    const { expandedState } = this.state;
    onToggle(e, groupId, !expandedState);
    this.onExpand(e, !expandedState);
  };

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { id, title, srText, children, className, isActive, groupId, isExpanded, onExpand, ...props } = this.props;
    const { expandedState } = this.state;

    return (
      <NavContext.Consumer>
        {(context: any) => (
          <li
            className={css(
              styles.navItem,
              expandedState && styles.modifiers.expanded,
              isActive && styles.modifiers.current,
              className
            )}
            onClick={(e: React.MouseEvent<HTMLLIElement, MouseEvent>) => this.handleToggle(e, context.onToggle)}
            {...props}
          >
            <a
              data-component="pf-nav-expandable"
              className={css(styles.navLink)}
              id={srText ? null : this.id}
              href="#"
              onClick={e => e.preventDefault()}
              onMouseDown={e => e.preventDefault()}
              aria-expanded={expandedState}
            >
              {title}
              <span className={css(styles.navToggle)}>
                <AngleRightIcon aria-hidden="true" />
              </span>
            </a>
            <section className={css(styles.navSubnav)} aria-labelledby={this.id} hidden={expandedState ? null : true}>
              {srText && (
                <h2 className={css(a11yStyles.screenReader)} id={this.id}>
                  {srText}
                </h2>
              )}
              <ul className={css(styles.navSimpleList)}>{children}</ul>
            </section>
          </li>
        )}
      </NavContext.Consumer>
    );
  }
}
