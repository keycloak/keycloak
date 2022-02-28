import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export interface SwitchProps extends Omit<React.HTMLProps<HTMLInputElement>, 'type' | 'onChange' | 'disabled' | 'label'> {
    /** id for the label. */
    id?: string;
    /** Additional classes added to the Switch */
    className?: string;
    /** Text value for the label when on */
    label?: string;
    /** Text value for the label when off */
    labelOff?: string;
    /** Flag to show if the Switch is checked. */
    isChecked?: boolean;
    /** Flag to show if the Switch is disabled. */
    isDisabled?: boolean;
    /** A callback for when the Switch selection changes. (isChecked, event) => {} */
    onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
    /** Adds accessible text to the Switch, and should describe the isChecked="true" state. When label is defined, aria-label should be set to the text string that is visible when isChecked is true. */
    'aria-label'?: string;
}
declare const SwitchWithOuiaContext: React.FunctionComponent<SwitchProps & InjectedOuiaProps>;
export { SwitchWithOuiaContext as Switch };
