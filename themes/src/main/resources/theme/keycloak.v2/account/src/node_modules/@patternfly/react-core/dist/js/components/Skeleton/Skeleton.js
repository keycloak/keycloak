"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Skeleton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const skeleton_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Skeleton/skeleton"));
const react_styles_1 = require("@patternfly/react-styles");
const Skeleton = (_a) => {
    var { className, width, height, fontSize, shape, screenreaderText } = _a, props = tslib_1.__rest(_a, ["className", "width", "height", "fontSize", "shape", "screenreaderText"]);
    const fontHeightClassName = fontSize
        ? Object.values(skeleton_1.default.modifiers).find(key => key === `pf-m-text-${fontSize}`)
        : undefined;
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(skeleton_1.default.skeleton, fontSize && fontHeightClassName, shape === 'circle' && skeleton_1.default.modifiers.circle, shape === 'square' && skeleton_1.default.modifiers.square, className) }, ((width || height) && {
        style: Object.assign({ '--pf-c-skeleton--Width': width ? width : undefined, '--pf-c-skeleton--Height': height ? height : undefined }, props.style)
    })),
        React.createElement("span", { className: "pf-u-screen-reader" }, screenreaderText)));
};
exports.Skeleton = Skeleton;
exports.Skeleton.displayName = 'Skeleton';
//# sourceMappingURL=Skeleton.js.map