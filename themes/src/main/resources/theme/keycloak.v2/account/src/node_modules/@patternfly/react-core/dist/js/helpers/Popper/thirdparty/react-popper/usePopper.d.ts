import { createPopper as defaultCreatePopper, Options as PopperOptions, VirtualElement } from '../popper-core/popper';
declare type $Shape<T extends object> = Partial<T>;
declare type Options = $Shape<PopperOptions & {
    createPopper: typeof defaultCreatePopper;
}>;
export declare const usePopper: (referenceElement: (Element | VirtualElement) | null | undefined, popperElement: HTMLElement | null | undefined, options?: Options) => {
    state: any;
    styles: {
        [key: string]: Partial<CSSStyleDeclaration>;
    };
    attributes: {
        [key: string]: {
            [key: string]: string;
        };
    };
    update: any;
    forceUpdate: any;
};
export {};
//# sourceMappingURL=usePopper.d.ts.map