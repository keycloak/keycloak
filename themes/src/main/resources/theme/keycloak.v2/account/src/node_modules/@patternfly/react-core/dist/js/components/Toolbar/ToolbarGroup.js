"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarGroup = exports.ToolbarGroupVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
const Page_1 = require("../Page/Page");
var ToolbarGroupVariant;
(function (ToolbarGroupVariant) {
    ToolbarGroupVariant["filter-group"] = "filter-group";
    ToolbarGroupVariant["icon-button-group"] = "icon-button-group";
    ToolbarGroupVariant["button-group"] = "button-group";
})(ToolbarGroupVariant = exports.ToolbarGroupVariant || (exports.ToolbarGroupVariant = {}));
class ToolbarGroupWithRef extends React.Component {
    render() {
        const _a = this.props, { visibility, visiblity, alignment, spacer, spaceItems, className, variant, children, innerRef } = _a, props = tslib_1.__rest(_a, ["visibility", "visiblity", "alignment", "spacer", "spaceItems", "className", "variant", "children", "innerRef"]);
        if (visiblity !== undefined) {
            // eslint-disable-next-line no-console
            console.warn('The ToolbarGroup visiblity prop has been deprecated. ' +
                'Please use the correctly spelled visibility prop instead.');
        }
        return (React.createElement(Page_1.PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbarGroup, variant && toolbar_1.default.modifiers[util_1.toCamel(variant)], util_1.formatBreakpointMods(visibility || visiblity, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(alignment, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(spacer, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(spaceItems, toolbar_1.default, '', getBreakpoint(width)), className) }, props, { ref: innerRef }), children))));
    }
}
exports.ToolbarGroup = React.forwardRef((props, ref) => (React.createElement(ToolbarGroupWithRef, Object.assign({}, props, { innerRef: ref }))));
//# sourceMappingURL=ToolbarGroup.js.map