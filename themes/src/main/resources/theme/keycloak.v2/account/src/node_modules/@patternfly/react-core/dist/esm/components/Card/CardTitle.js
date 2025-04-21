import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { CardContext } from './Card';
export const CardTitle = (_a) => {
    var { children = null, className = '', component = 'div' } = _a, props = __rest(_a, ["children", "className", "component"]);
    const { cardId, registerTitleId } = React.useContext(CardContext);
    const Component = component;
    const titleId = cardId ? `${cardId}-title` : '';
    React.useEffect(() => {
        registerTitleId(titleId);
        return () => registerTitleId('');
    }, [registerTitleId, titleId]);
    return (React.createElement(Component, Object.assign({ className: css(styles.cardTitle, className), id: titleId || undefined }, props), children));
};
CardTitle.displayName = 'CardTitle';
//# sourceMappingURL=CardTitle.js.map