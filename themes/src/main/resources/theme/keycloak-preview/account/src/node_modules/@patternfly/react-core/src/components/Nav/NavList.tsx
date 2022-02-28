import * as React from 'react';
import { NavVariants } from './NavVariants';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { isElementInView } from '../../helpers/util';
import { NavContext } from './Nav';

export interface NavListProps
  extends React.DetailedHTMLProps<React.HTMLAttributes<HTMLUListElement>, HTMLUListElement> {
  /** Children nodes */
  children?: React.ReactNode;
  /** Additional classes added to the list */
  className?: string;
  /** Indicates the list type. */
  variant?: 'default' | 'simple' | 'horizontal' | 'tertiary';
  /** aria-label for the left scroll button */
  ariaLeftScroll?: string;
  /** aria-label for the right scroll button */
  ariaRightScroll?: string;
}

export class NavList extends React.Component<NavListProps> {
  static contextType = NavContext;

  static defaultProps: NavListProps = {
    variant: 'default',
    children: null as React.ReactNode,
    className: '',
    ariaLeftScroll: 'Scroll left',
    ariaRightScroll: 'Scroll right'
  };

  navList = React.createRef<HTMLUListElement>();

  handleScrollButtons = () => {
    if (this.navList.current) {
      const { updateScrollButtonState } = this.context;
      const container = this.navList.current;
      // get first element and check if it is in view
      const showLeftScrollButton = !isElementInView(container, container.firstChild as HTMLElement, false);

      // get last element and check if it is in view
      const showRightScrollButton = !isElementInView(container, container.lastChild as HTMLElement, false);

      updateScrollButtonState({
        showLeftScrollButton,
        showRightScrollButton
      });
    }
  };

  scrollLeft = () => {
    // find first Element that is fully in view on the left, then scroll to the element before it
    if (this.navList.current) {
      const container = this.navList.current;
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
    if (this.navList.current) {
      const container = this.navList.current;
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
    const { variant } = this.props;
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;
    if (isHorizontal) {
      window.addEventListener('resize', this.handleScrollButtons, false);
      // call the handle resize function to check if scroll buttons should be shown
      this.handleScrollButtons();
    }
  }

  componentWillUnmount() {
    const { variant } = this.props;
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;
    if (isHorizontal) {
      document.removeEventListener('resize', this.handleScrollButtons, false);
    }
  }

  render() {
    const { variant, children, className, ariaLeftScroll, ariaRightScroll, ...props } = this.props;
    const variantStyle = {
      [NavVariants.default]: styles.navList,
      [NavVariants.simple]: styles.navSimpleList,
      [NavVariants.horizontal]: styles.navHorizontalList,
      [NavVariants.tertiary]: styles.navTertiaryList
    };
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;

    return (
      <>
        {isHorizontal && (
          <button className={css(styles.navScrollButton)} aria-label={ariaLeftScroll} onClick={this.scrollLeft}>
            <AngleLeftIcon />
          </button>
        )}
        <ul ref={this.navList} className={css(variantStyle[variant], className)} {...props}>
          {children}
        </ul>
        {isHorizontal && (
          <button className={css(styles.navScrollButton)} aria-label={ariaRightScroll} onClick={this.scrollRight}>
            <AngleRightIcon />
          </button>
        )}
      </>
    );
  }
}
