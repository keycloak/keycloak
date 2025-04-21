"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BackgroundImage = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const c_background_image_BackgroundImage_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage'));
const c_background_image_BackgroundImage_2x_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x'));
const c_background_image_BackgroundImage_sm_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm'));
const c_background_image_BackgroundImage_sm_2x_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x'));
const c_background_image_BackgroundImage_lg_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg'));
const c_background_image_Filter_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_background_image_Filter'));
const react_styles_1 = require("@patternfly/react-styles");
const background_image_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/BackgroundImage/background-image"));
const defaultFilter = (React.createElement("filter", null,
    React.createElement("feColorMatrix", { type: "matrix", values: "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0" }),
    React.createElement("feComponentTransfer", { colorInterpolationFilters: "sRGB", result: "duotone" },
        React.createElement("feFuncR", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncG", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncB", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncA", { type: "table", tableValues: "0 1" }))));
let filterCounter = 0;
const BackgroundImage = (_a) => {
    var { className, src, filter = defaultFilter } = _a, props = tslib_1.__rest(_a, ["className", "src", "filter"]);
    const getUrlValue = (size) => {
        if (typeof src === 'string') {
            return `url(${src})`;
        }
        else if (typeof src === 'object') {
            return `url(${src[size]})`;
        }
        return '';
    };
    const filterNum = React.useMemo(() => filterCounter++, []);
    const filterId = `patternfly-background-image-filter-overlay${filterNum}`;
    const style = {
        [c_background_image_BackgroundImage_1.default.name]: getUrlValue('xs'),
        [c_background_image_BackgroundImage_2x_1.default.name]: getUrlValue('xs2x'),
        [c_background_image_BackgroundImage_sm_1.default.name]: getUrlValue('sm'),
        [c_background_image_BackgroundImage_sm_2x_1.default.name]: getUrlValue('sm2x'),
        [c_background_image_BackgroundImage_lg_1.default.name]: getUrlValue('lg'),
        [c_background_image_Filter_1.default.name]: `url(#${filterId})`
    };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(background_image_1.default.backgroundImage, className), style: style }, props),
        React.createElement("svg", { xmlns: "http://www.w3.org/2000/svg", className: "pf-c-background-image__filter", width: "0", height: "0" }, React.cloneElement(filter, { id: filterId }))));
};
exports.BackgroundImage = BackgroundImage;
exports.BackgroundImage.displayName = 'BackgroundImage';
//# sourceMappingURL=BackgroundImage.js.map