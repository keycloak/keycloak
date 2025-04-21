"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BackToTop = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const back_to_top_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/BackToTop/back-to-top"));
const react_styles_1 = require("@patternfly/react-styles");
const angle_up_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-up-icon'));
const util_1 = require("../../helpers/util");
const Button_1 = require("../Button");
const BackToTopBase = (_a) => {
    var { className, title = 'Back to top', innerRef, scrollableSelector, isAlwaysVisible = false } = _a, props = tslib_1.__rest(_a, ["className", "title", "innerRef", "scrollableSelector", "isAlwaysVisible"]);
    const [visible, setVisible] = React.useState(isAlwaysVisible);
    React.useEffect(() => {
        setVisible(isAlwaysVisible);
    }, [isAlwaysVisible]);
    const [scrollElement, setScrollElement] = React.useState(null);
    const toggleVisible = () => {
        const scrolled = scrollElement.scrollY ? scrollElement.scrollY : scrollElement.scrollTop;
        if (!isAlwaysVisible) {
            if (scrolled > 400) {
                setVisible(true);
            }
            else {
                setVisible(false);
            }
        }
    };
    React.useEffect(() => {
        const hasScrollSpy = Boolean(scrollableSelector);
        if (hasScrollSpy) {
            const scrollEl = document.querySelector(scrollableSelector);
            if (!util_1.canUseDOM || !(scrollEl instanceof HTMLElement)) {
                return;
            }
            setScrollElement(scrollEl);
            scrollEl.addEventListener('scroll', toggleVisible);
            return () => {
                scrollEl.removeEventListener('scroll', toggleVisible);
            };
        }
        else {
            if (!util_1.canUseDOM) {
                return;
            }
            const scrollEl = window;
            setScrollElement(scrollEl);
            scrollEl.addEventListener('scroll', toggleVisible);
            return () => {
                scrollEl.removeEventListener('scroll', toggleVisible);
            };
        }
    }, [scrollableSelector, toggleVisible]);
    const handleClick = () => {
        scrollElement.scrollTo({ top: 0, behavior: 'smooth' });
    };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(back_to_top_1.default.backToTop, !visible && back_to_top_1.default.modifiers.hidden, className), ref: innerRef, onClick: handleClick }, props),
        React.createElement(Button_1.Button, { variant: "primary", icon: React.createElement(angle_up_icon_1.default, { "aria-hidden": "true" }), iconPosition: "right" }, title)));
};
exports.BackToTop = React.forwardRef((props, ref) => (React.createElement(BackToTopBase, Object.assign({ innerRef: ref }, props))));
exports.BackToTop.displayName = 'BackToTop';
//# sourceMappingURL=BackToTop.js.map