"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TabContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const tab_content_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TabContent/tab-content"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const TabsContext_1 = require("./TabsContext");
const variantStyle = {
    default: '',
    light300: tab_content_1.default.modifiers.light_300
};
const TabContentBase = (_a) => {
    var { id, activeKey, 'aria-label': ariaLabel, child, children, className, eventKey, innerRef, ouiaId, ouiaSafe } = _a, props = tslib_1.__rest(_a, ["id", "activeKey", 'aria-label', "child", "children", "className", "eventKey", "innerRef", "ouiaId", "ouiaSafe"]);
    if (children || child) {
        let labelledBy;
        if (ariaLabel) {
            labelledBy = null;
        }
        else {
            labelledBy = children ? `pf-tab-${eventKey}-${id}` : `pf-tab-${child.props.eventKey}-${id}`;
        }
        return (React.createElement(TabsContext_1.TabsContextConsumer, null, ({ variant }) => (React.createElement("section", Object.assign({ ref: innerRef, hidden: children ? null : child.props.eventKey !== activeKey, className: children
                ? react_styles_1.css('pf-c-tab-content', className, variantStyle[variant])
                : react_styles_1.css('pf-c-tab-content', child.props.className, variantStyle[variant]), id: children ? id : `pf-tab-section-${child.props.eventKey}-${id}`, "aria-label": ariaLabel, "aria-labelledby": labelledBy, role: "tabpanel", tabIndex: 0 }, helpers_1.getOUIAProps('TabContent', ouiaId, ouiaSafe), props), children || child.props.children))));
    }
    return null;
};
exports.TabContent = React.forwardRef((props, ref) => (React.createElement(TabContentBase, Object.assign({}, props, { innerRef: ref }))));
//# sourceMappingURL=TabContent.js.map