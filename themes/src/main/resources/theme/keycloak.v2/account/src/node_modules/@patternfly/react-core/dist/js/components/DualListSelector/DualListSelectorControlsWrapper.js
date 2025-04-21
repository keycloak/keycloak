"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorControlsWrapper = exports.DualListSelectorControlsWrapperBase = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const DualListSelectorControlsWrapperBase = (_a) => {
    var { innerRef, children = null, className, 'aria-label': ariaLabel = 'Controls for moving options between lists' } = _a, props = tslib_1.__rest(_a, ["innerRef", "children", "className", 'aria-label']);
    const wrapperRef = innerRef || React.useRef(null);
    // Adds keyboard navigation to the dynamically built dual list selector controls. Works when controls are dynamically built
    // as well as when they are passed in via children.
    const handleKeys = (event) => {
        if (!wrapperRef.current ||
            (wrapperRef.current !== event.target.closest('.pf-c-dual-list-selector__controls') &&
                !Array.from(wrapperRef.current.getElementsByClassName('pf-c-dual-list-selector__controls')).includes(event.target.closest('.pf-c-dual-list-selector__controls')))) {
            return;
        }
        event.stopImmediatePropagation();
        const controls = Array.from(wrapperRef.current.getElementsByTagName('BUTTON')).filter(el => !el.classList.contains('pf-m-disabled'));
        const activeElement = document.activeElement;
        helpers_1.handleArrows(event, controls, (element) => activeElement.contains(element), (element) => element, undefined, undefined, true, false);
    };
    React.useEffect(() => {
        window.addEventListener('keydown', handleKeys);
        return () => {
            window.removeEventListener('keydown', handleKeys);
        };
    }, [wrapperRef.current]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorControls, className), tabIndex: 0, ref: wrapperRef, "aria-label": ariaLabel }, props), children));
};
exports.DualListSelectorControlsWrapperBase = DualListSelectorControlsWrapperBase;
exports.DualListSelectorControlsWrapperBase.displayName = 'DualListSelectorControlsWrapperBase';
exports.DualListSelectorControlsWrapper = React.forwardRef((props, ref) => (React.createElement(exports.DualListSelectorControlsWrapperBase, Object.assign({ innerRef: ref }, props))));
exports.DualListSelectorControlsWrapper.displayName = 'DualListSelectorControlsWrapper';
//# sourceMappingURL=DualListSelectorControlsWrapper.js.map