"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NavList = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const nav_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Nav/nav"));
const react_styles_1 = require("@patternfly/react-styles");
const angle_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-left-icon'));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const util_1 = require("../../helpers/util");
const Nav_1 = require("./Nav");
const PageSidebar_1 = require("../Page/PageSidebar");
const resizeObserver_1 = require("../../helpers/resizeObserver");
class NavList extends React.Component {
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
                const scrollViewAtStart = util_1.isElementInView(container, container.firstChild, false);
                const scrollViewAtEnd = util_1.isElementInView(container, container.lastChild, false);
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
                    if (util_1.isElementInView(container, childrenArr[i], false)) {
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
                    if (util_1.isElementInView(container, childrenArr[i], false)) {
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
        this.observer = resizeObserver_1.getResizeObserver(this.navList.current, this.handleScrollButtons);
        this.handleScrollButtons();
    }
    componentWillUnmount() {
        this.observer();
    }
    render() {
        const _a = this.props, { children, className, ariaLeftScroll, ariaRightScroll } = _a, props = tslib_1.__rest(_a, ["children", "className", "ariaLeftScroll", "ariaRightScroll"]);
        const { scrollViewAtStart, scrollViewAtEnd } = this.state;
        return (React.createElement(Nav_1.NavContext.Consumer, null, ({ isHorizontal }) => (React.createElement(PageSidebar_1.PageSidebarContext.Consumer, null, ({ isNavOpen }) => (React.createElement(React.Fragment, null,
            isHorizontal && (React.createElement("button", { className: react_styles_1.css(nav_1.default.navScrollButton), "aria-label": ariaLeftScroll, onClick: this.scrollLeft, disabled: scrollViewAtStart, tabIndex: isNavOpen ? null : -1 },
                React.createElement(angle_left_icon_1.default, null))),
            React.createElement("ul", Object.assign({ ref: this.navList, className: react_styles_1.css(nav_1.default.navList, className), onScroll: this.handleScrollButtons }, props), children),
            isHorizontal && (React.createElement("button", { className: react_styles_1.css(nav_1.default.navScrollButton), "aria-label": ariaRightScroll, onClick: this.scrollRight, disabled: scrollViewAtEnd, tabIndex: isNavOpen ? null : -1 },
                React.createElement(angle_right_icon_1.default, null)))))))));
    }
}
exports.NavList = NavList;
NavList.displayName = 'NavList';
NavList.contextType = Nav_1.NavContext;
NavList.defaultProps = {
    ariaLeftScroll: 'Scroll left',
    ariaRightScroll: 'Scroll right'
};
//# sourceMappingURL=NavList.js.map