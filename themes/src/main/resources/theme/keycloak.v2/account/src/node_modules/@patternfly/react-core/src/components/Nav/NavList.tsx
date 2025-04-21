import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { isElementInView } from '../../helpers/util';
import { NavContext } from './Nav';
import { PageSidebarContext } from '../Page/PageSidebar';
import { getResizeObserver } from '../../helpers/resizeObserver';

export interface NavListProps
  extends React.DetailedHTMLProps<React.HTMLAttributes<HTMLUListElement>, HTMLUListElement> {
  /** Children nodes */
  children?: React.ReactNode;
  /** Additional classes added to the list */
  className?: string;
  /** Aria-label for the left scroll button */
  ariaLeftScroll?: string;
  /** Aria-label for the right scroll button */
  ariaRightScroll?: string;
}

export class NavList extends React.Component<NavListProps> {
  static displayName = 'NavList';
  static contextType = NavContext;
  context!: React.ContextType<typeof NavContext>;
  static defaultProps: NavListProps = {
    ariaLeftScroll: 'Scroll left',
    ariaRightScroll: 'Scroll right'
  };

  state = {
    scrollViewAtStart: false,
    scrollViewAtEnd: false
  };

  navList = React.createRef<HTMLUListElement>();
  observer: any = () => {};

  handleScrollButtons = () => {
    const container = this.navList.current;
    if (container) {
      // check if it elements are in view
      const scrollViewAtStart = isElementInView(container, container.firstChild as HTMLElement, false);
      const scrollViewAtEnd = isElementInView(container, container.lastChild as HTMLElement, false);
      this.setState({
        scrollViewAtStart,
        scrollViewAtEnd
      });
      this.context.updateIsScrollable(!scrollViewAtStart || !scrollViewAtEnd);
    }
  };

  scrollLeft = () => {
    // find first Element that is fully in view on the left, then scroll to the element before it
    const container = this.navList.current;
    if (container) {
      const childrenArr = Array.from(container.children);
      let firstElementInView: Element;
      let lastElementOutOfView: Element;
      for (let i = 0; i < childrenArr.length && !firstElementInView; i++) {
        if (isElementInView(container, childrenArr[i] as HTMLElement, false)) {
          firstElementInView = childrenArr[i];
          lastElementOutOfView = childrenArr[i - 1];
        }
      }
      if (lastElementOutOfView) {
        container.scrollLeft -= lastElementOutOfView.scrollWidth;
      }
      this.handleScrollButtons();
    }
  };

  scrollRight = () => {
    // find last Element that is fully in view on the right, then scroll to the element after it
    const container = this.navList.current;
    if (container) {
      const childrenArr = Array.from(container.children);
      let lastElementInView: Element;
      let firstElementOutOfView: Element;
      for (let i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
        if (isElementInView(container, childrenArr[i] as HTMLElement, false)) {
          lastElementInView = childrenArr[i];
          firstElementOutOfView = childrenArr[i + 1];
        }
      }
      if (firstElementOutOfView) {
        container.scrollLeft += firstElementOutOfView.scrollWidth;
      }
      this.handleScrollButtons();
    }
  };

  componentDidMount() {
    this.observer = getResizeObserver(this.navList.current, this.handleScrollButtons);
    this.handleScrollButtons();
  }

  componentWillUnmount() {
    this.observer();
  }

  render() {
    const { children, className, ariaLeftScroll, ariaRightScroll, ...props } = this.props;
    const { scrollViewAtStart, scrollViewAtEnd } = this.state;

    return (
      <NavContext.Consumer>
        {({ isHorizontal }) => (
          <PageSidebarContext.Consumer>
            {({ isNavOpen }) => (
              <React.Fragment>
                {isHorizontal && (
                  <button
                    className={css(styles.navScrollButton)}
                    aria-label={ariaLeftScroll}
                    onClick={this.scrollLeft}
                    disabled={scrollViewAtStart}
                    tabIndex={isNavOpen ? null : -1}
                  >
                    <AngleLeftIcon />
                  </button>
                )}
                <ul
                  ref={this.navList}
                  className={css(styles.navList, className)}
                  onScroll={this.handleScrollButtons}
                  {...props}
                >
                  {children}
                </ul>
                {isHorizontal && (
                  <button
                    className={css(styles.navScrollButton)}
                    aria-label={ariaRightScroll}
                    onClick={this.scrollRight}
                    disabled={scrollViewAtEnd}
                    tabIndex={isNavOpen ? null : -1}
                  >
                    <AngleRightIcon />
                  </button>
                )}
              </React.Fragment>
            )}
          </PageSidebarContext.Consumer>
        )}
      </NavContext.Consumer>
    );
  }
}
