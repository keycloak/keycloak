import { __rest } from "tslib";
import * as React from 'react';
import cssVar from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage';
import cssVarName2x from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_2x';
import cssVarNameSm from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_sm';
import cssVarNameSm2x from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_sm_2x';
import cssVarNameLg from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_lg';
import cssVarNameFilter from '@patternfly/react-tokens/dist/esm/c_background_image_Filter';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/BackgroundImage/background-image';
const defaultFilter = (React.createElement("filter", null,
    React.createElement("feColorMatrix", { type: "matrix", values: "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0" }),
    React.createElement("feComponentTransfer", { colorInterpolationFilters: "sRGB", result: "duotone" },
        React.createElement("feFuncR", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncG", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncB", { type: "table", tableValues: "0.086274509803922 0.43921568627451" }),
        React.createElement("feFuncA", { type: "table", tableValues: "0 1" }))));
let filterCounter = 0;
export const BackgroundImage = (_a) => {
    var { className, src, filter = defaultFilter } = _a, props = __rest(_a, ["className", "src", "filter"]);
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
        [cssVar.name]: getUrlValue('xs'),
        [cssVarName2x.name]: getUrlValue('xs2x'),
        [cssVarNameSm.name]: getUrlValue('sm'),
        [cssVarNameSm2x.name]: getUrlValue('sm2x'),
        [cssVarNameLg.name]: getUrlValue('lg'),
        [cssVarNameFilter.name]: `url(#${filterId})`
    };
    return (React.createElement("div", Object.assign({ className: css(styles.backgroundImage, className), style: style }, props),
        React.createElement("svg", { xmlns: "http://www.w3.org/2000/svg", className: "pf-c-background-image__filter", width: "0", height: "0" }, React.cloneElement(filter, { id: filterId }))));
};
BackgroundImage.displayName = 'BackgroundImage';
//# sourceMappingURL=BackgroundImage.js.map