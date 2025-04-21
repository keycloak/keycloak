import * as React from 'react';
export interface SliderStepProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
    /** Additional classes added to the slider steps. */
    className?: string;
    /** Step value **/
    value?: number;
    /** Step label **/
    label?: string;
    /** Flag indicating that the tick should be hidden */
    isTickHidden?: boolean;
    /** Flag indicating that the label should be hidden */
    isLabelHidden?: boolean;
    /** Flag indicating the step is active */
    isActive?: boolean;
}
export declare const SliderStep: React.FunctionComponent<SliderStepProps>;
//# sourceMappingURL=SliderStep.d.ts.map