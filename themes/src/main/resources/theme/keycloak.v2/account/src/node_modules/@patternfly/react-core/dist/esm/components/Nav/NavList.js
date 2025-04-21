import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { isElementInView } from '../../helpers/util';
import { NavContext } from './Nav';
import { PageSidebarContext } from '../Page/PageSidebar';
import { getResizeObserver } from '../../helpers/resizeObserver';
export class NavList extends React.Component {
    constructor() {
        super(...arguments);
        this.state = {
            scrollViewAtStart: false,
            scrollViewAtEnd: false
        };
        this.navList = React.createRef();
        this.observer = () => { };
        this.handleScrollButtons = () => {
            const container = this.navList.current;
            if (container) {
                // check if it elements are in view
                const scrollViewAtStart = isElementInView(container, container.firstChild, false);
                const scrollViewAtEnd = isElementInView(container, container.lastChild, false);
                this.setState({
                    scrollViewAtStart,
                    scrollViewAtEnd
                });
                this.context.updateIsScrollable(!scrollViewAtStart || !scrollViewAtEnd);
            }
        };
        this.scrollLeft = () => {
            // find first Element that is fully in view on the left, then scroll to the element before it
            const container = this.navList.current;
            if (container) {
                const childrenArr = Array.from(container.children);
                let firstElementInView;
                let lastElementOutOfView;
                for (let i = 0; i < childrenArr.length && !firstElementInView; i++) {
                    if (isElementInView(container, childrenArr[i], false)) {
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
        this.scrollRight = () => {
            // find last Element that is fully in view on the right, then scroll to the element after it
            const container = this.navList.current;
            if (container) {
                const childrenArr = Array.from(container.children);
                let lastElementInView;
                let firstElementOutOfView;
                for (let i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
                    if (isElementInView(container, childrenArr[i], false)) {
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
    }
    componentDidMount() {
        this.observer = getResizeObserver(this.navList.current, this.handleScrollButtons);
        this.handleScrollButtons();
    }
    componentWillUnmount() {
        this.observer();
    }
    render() {
        const _a = this.props, { children, className, ariaLeftScroll, ariaRightScroll } = _a, props = __rest(_a, ["children", "className", "ariaLeftScroll", "ariaRightScroll"]);
        const { scrollViewAtStart, scrollViewAtEnd } = this.state;
        return (React.createElement(NavContext.Consumer, null, ({ isHorizontal }) => (React.createElement(PageSidebarContext.Consumer, null, ({ isNavOpen }) => (React.createElement(React.Fragment, null,
            isHorizontal && (React.createElement("button", { className: css(styles.navScrollButton), "aria-label": ariaLeftScroll, onClick: this.scrollLeft, disabled: scrollViewAtStart, tabIndex: isNavOpen ? null : -1 },
                React.createElement(AngleLeftIcon, null))),
            React.createElement("ul", Object.assign({ ref: this.navList, className: css(styles.navList, className), onScroll: this.handleScrollButtons }, props), children),
            isHorizontal && (React.createElement("button", { className: css(styles.navScrollButton), "aria-label": ariaRightScroll, onClick: this.scrollRight, disabled: scrollViewAtEnd, tabIndex: isNavOpen ? null : -1 },
                React.createElement(AngleRightIcon, null)))))))));
    }
}
NavList.displayName = 'NavList';
NavList.contextType = NavContext;
NavList.defaultProps = {
    ariaLeftScroll: 'Scroll left',
    ariaRightScroll: 'Scroll right'
};
//# sourceMappingURL=NavList.js.map