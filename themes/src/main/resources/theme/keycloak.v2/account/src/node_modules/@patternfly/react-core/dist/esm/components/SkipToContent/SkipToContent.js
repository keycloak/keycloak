import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/SkipToContent/skip-to-content';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
export class SkipToContent extends React.Component {
    constructor() {
        super(...arguments);
        this.componentRef = React.createRef();
    }
    componentDidMount() {
        if (this.props.show && this.componentRef.current) {
            this.componentRef.current.focus();
        }
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { children, className, href, show, type } = _a, rest = __rest(_a, ["children", "className", "href", "show", "type"]);
        return (React.createElement("a", Object.assign({}, rest, { className: css(buttonStyles.button, buttonStyles.modifiers.primary, styles.skipToContent, className), ref: this.componentRef, href: href }), children));
    }
}
SkipToContent.displayName = 'SkipToContent';
SkipToContent.defaultProps = {
    show: false
};
//# sourceMappingURL=SkipToContent.js.map