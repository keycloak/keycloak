import * as React from 'react';
export interface SliderStepObject {
    /** Value of the step. This value is a percentage of the slider where the  tick is drawn. */
    value: number;
    /** The display label for the step value. This is also used for the aria-valuetext */
    label: string;
    /** Flag to hide the label */
    isLabelHidden?: boolean;
}
export interface SliderProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
    /** Additional classes added to the spinner. */
    className?: string;
    /** Current value  */
    value?: number;
    /** Flag indicating if the slider is is discrete for custom steps.  This will cause the slider to snap to the closest value. */
    areCustomStepsContinuous?: boolean;
    /** Adds disabled styling and disables the slider and the input component is present */
    isDisabled?: boolean;
    /** The step interval*/
    step?: number;
    /** Minimum permitted value */
    min?: number;
    /** The maximum permitted value */
    max?: number;
    /** Flag to indicate if boundaries should be shown for slider that does not have custom steps */
    showBoundaries?: boolean;
    /** Flag to indicate if ticks should be shown for slider that does not have custom steps  */
    showTicks?: boolean;
    /** Array of custom slider step objects (value and label of each step) for the slider. */
    customSteps?: SliderStepObject[];
    /** Flag to show value input field */
    isInputVisible?: boolean;
    /** Value displayed in the input field */
    inputValue?: number;
    /** Aria label for the input field */
    inputAriaLabel?: string;
    thumbAriaLabel?: string;
    hasTooltipOverThumb?: boolean;
    /** Label that is place after the input field */
    inputLabel?: string | number;
    /** Position of the input */
    inputPosition?: 'aboveThumb' | 'right';
    /** Value change callback. This is called when the slider value changes */
    onChange?: (value: number, inputValue?: number, setLocalInputValue?: React.Dispatch<React.SetStateAction<number>>) => void;
    /** Actions placed to the left of the slider */
    leftActions?: React.ReactNode;
    /** Actions placed to the right of the slider */
    rightActions?: React.ReactNode;
    /** One or more id's to use for the slider thumb description */
    'aria-describedby'?: string;
    /** One or more id's to use for the slider thumb label */
    'aria-labelledby'?: string;
}
export declare const Slider: React.FunctionComponent<SliderProps>;
//# sourceMappingURL=Slider.d.ts.map