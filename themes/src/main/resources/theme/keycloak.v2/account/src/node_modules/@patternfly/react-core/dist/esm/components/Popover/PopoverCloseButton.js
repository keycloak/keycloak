import { __rest } from "tslib";
import * as React from 'react';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { FindRefWrapper } from '../../helpers/Popper/FindRefWrapper';
export const PopoverCloseButton = (_a) => {
    var { onClose = () => undefined } = _a, props = __rest(_a, ["onClose"]);
    const [closeButtonElement, setCloseButtonElement] = React.useState(null);
    React.useEffect(() => {
        closeButtonElement && closeButtonElement.addEventListener('click', onClose, false);
        return () => {
            closeButtonElement && closeButtonElement.removeEventListener('click', onClose, false);
        };
    }, [closeButtonElement]);
    return (React.createElement(FindRefWrapper, { onFoundRef: (foundRef) => setCloseButtonElement(foundRef) },
        React.createElement(Button, Object.assign({ variant: "plain", "aria-label": true }, props, { style: { pointerEvents: 'auto' } }),
            React.createElement(TimesIcon, null))));
};
PopoverCloseButton.displayName = 'PopoverCloseButton';
//# sourceMappingURL=PopoverCloseButton.js.map