import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { OUIAProps } from '../../helpers';
export interface RadioProps extends Omit<React.HTMLProps<HTMLInputElement>, 'disabled' | 'label' | 'onChange' | 'type'>, OUIAProps {
    /** Additional classes added to the radio. */
    className?: string;
    /** Id of the radio. */
    id: string;
    /** Flag to show if the radio label is wrapped on small screen. */
    isLabelWrapped?: boolean;
    /** Flag to show if the radio label is shown before the radio button. */
    isLabelBeforeButton?: boolean;
    /** Flag to show if the radio is checked. */
    checked?: boolean;
    /** Flag to show if the radio is checked. */
    isChecked?: boolean;
    /** Flag to show if the radio is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the radio selection is valid or invalid. */
    isValid?: boolean;
    /** Label text of the radio. */
    label?: React.ReactNode;
    /** Name for group of radios */
    name: string;
    /** A callback for when the radio selection changes. */
    onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
    /** Aria label for the radio. */
    'aria-label'?: string;
    /** Description text of the radio. */
    description?: React.ReactNode;
    /** Body of the radio. */
    body?: React.ReactNode;
}
export declare class Radio extends React.Component<RadioProps, {
    ouiaStateId: string;
}> {
    static displayName: string;
    static defaultProps: PickOptional<RadioProps>;
    constructor(props: RadioProps);
    handleChange: (event: React.FormEvent<HTMLInputElement>) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=Radio.d.ts.map