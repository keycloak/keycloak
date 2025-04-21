/**
 * Added types from tippy.js and popper.js to preserve backwards compatibility
 * Can remove in next breaking change release
 */
/**
 * This kind of namespace declaration is not necessary, but is kept here for backwards-compatibility with
 * popper.js 1.x. It can be removed in 2.x so that the default export is simply the Popper class
 * and all the types / interfaces are top-level named exports.
 */
declare namespace Popper {
    type Position = 'top' | 'right' | 'bottom' | 'left';
    type Placement = 'auto-start' | 'auto' | 'auto-end' | 'top-start' | 'top' | 'top-end' | 'right-start' | 'right' | 'right-end' | 'bottom-end' | 'bottom' | 'bottom-start' | 'left-end' | 'left' | 'left-start';
    type Boundary = 'scrollParent' | 'viewport' | 'window';
    type Behavior = 'flip' | 'clockwise' | 'counterclockwise';
    type ModifierFn = (data: Data, options: Object) => Data;
    interface Attributes {
        'x-out-of-boundaries': '' | false;
        'x-placement': Placement;
    }
    interface Padding {
        top?: number;
        bottom?: number;
        left?: number;
        right?: number;
    }
    interface BaseModifier {
        order?: number;
        enabled?: boolean;
        fn?: ModifierFn;
    }
    interface Modifiers {
        shift?: BaseModifier;
        offset?: BaseModifier & {
            offset?: number | string;
        };
        preventOverflow?: BaseModifier & {
            priority?: Position[];
            padding?: number | Padding;
            boundariesElement?: Boundary | Element;
            escapeWithReference?: boolean;
        };
        keepTogether?: BaseModifier;
        arrow?: BaseModifier & {
            element?: string | Element;
        };
        flip?: BaseModifier & {
            behavior?: Behavior | Position[];
            padding?: number | Padding;
            boundariesElement?: Boundary | Element;
            flipVariations?: boolean;
            flipVariationsByContent?: boolean;
        };
        inner?: BaseModifier;
        hide?: BaseModifier;
        applyStyle?: BaseModifier & {
            onLoad?: Function;
            gpuAcceleration?: boolean;
        };
        computeStyle?: BaseModifier & {
            gpuAcceleration?: boolean;
            x?: 'bottom' | 'top';
            y?: 'left' | 'right';
        };
        [name: string]: (BaseModifier & Record<string, any>) | undefined;
    }
    interface Offset {
        top: number;
        left: number;
        width: number;
        height: number;
    }
    interface Data {
        instance: Popper;
        placement: Placement;
        originalPlacement: Placement;
        flipped: boolean;
        hide: boolean;
        arrowElement: Element;
        styles: CSSStyleDeclaration;
        arrowStyles: CSSStyleDeclaration;
        attributes: Attributes;
        boundaries: Object;
        offsets: {
            popper: Offset;
            reference: Offset;
            arrow: {
                top: number;
                left: number;
            };
        };
    }
    interface PopperOptions {
        placement?: Placement;
        positionFixed?: boolean;
        eventsEnabled?: boolean;
        modifiers?: Modifiers;
        removeOnDestroy?: boolean;
        onCreate?(data: Data): void;
        onUpdate?(data: Data): void;
    }
    interface ReferenceObject {
        clientHeight: number;
        clientWidth: number;
        referenceNode?: Node;
        getBoundingClientRect(): ClientRect;
    }
}
export declare type Padding = Popper.Padding;
export declare type Position = Popper.Position;
export declare type Placement = Popper.Placement;
export declare type Boundary = Popper.Boundary;
export declare type Behavior = Popper.Behavior;
export declare type ModifierFn = Popper.ModifierFn;
export declare type BaseModifier = Popper.BaseModifier;
export declare type Modifiers = Popper.Modifiers;
export declare type Offset = Popper.Offset;
export declare type Data = Popper.Data;
export declare type PopperOptions = Popper.PopperOptions;
export declare type ReferenceObject = Popper.ReferenceObject;
declare class Popper {
    static modifiers: (BaseModifier & {
        name: string;
    })[];
    static placements: Placement[];
    static Defaults: PopperOptions;
    options: PopperOptions;
    popper: Element;
    reference: Element | ReferenceObject;
    constructor(reference: Element | ReferenceObject, popper: Element, options?: PopperOptions);
    destroy(): void;
    update(): void;
    scheduleUpdate(): void;
    enableEventListeners(): void;
    disableEventListeners(): void;
}
export default Popper;
//# sourceMappingURL=DeprecatedPopperTypes.d.ts.map