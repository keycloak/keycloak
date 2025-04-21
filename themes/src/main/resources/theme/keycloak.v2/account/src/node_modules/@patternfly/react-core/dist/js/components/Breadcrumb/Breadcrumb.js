"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Breadcrumb = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const breadcrumb_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Breadcrumb/breadcrumb"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const Breadcrumb = (_a) => {
    var { children = null, className = '', 'aria-label': ariaLabel = 'Breadcrumb', ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ["children", "className", 'aria-label', "ouiaId", "ouiaSafe"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.Breadcrumb.displayName, ouiaId, ouiaSafe);
    return (React.createElement("nav", Object.assign({}, props, { "aria-label": ariaLabel, className: react_styles_1.css(breadcrumb_1.default.breadcrumb, className) }, ouiaProps),
        React.createElement("ol", { className: breadcrumb_1.default.breadcrumbList }, React.Children.map(children, (child, index) => {
            const showDivider = index > 0;
            if (React.isValidElement(child)) {
                return React.cloneElement(child, { showDivider });
            }
            return child;
        }))));
};
exports.Breadcrumb = Breadcrumb;
exports.Breadcrumb.displayName = 'Breadcrumb';
//# sourceMappingURL=Breadcrumb.js.map