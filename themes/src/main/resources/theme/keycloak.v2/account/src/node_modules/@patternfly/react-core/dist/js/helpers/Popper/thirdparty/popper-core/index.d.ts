import { OptionsGeneric, Modifier, Instance, VirtualElement } from './types';
import detectOverflow from './utils/detectOverflow';
export * from './types';
export * from './enums';
interface PopperGeneratorArgs {
    defaultModifiers?: Modifier<any, any>[];
    defaultOptions?: Partial<OptionsGeneric<any>>;
}
/**
 * @param generatorOptions
 */
export declare function popperGenerator(generatorOptions?: PopperGeneratorArgs): <TModifier extends Partial<Modifier<any, any>>>(reference: Element | VirtualElement, popper: HTMLElement, options?: Partial<OptionsGeneric<TModifier>>) => Instance;
export declare const createPopper: <TModifier extends Partial<Modifier<any, any>>>(reference: Element | VirtualElement, popper: HTMLElement, options?: Partial<OptionsGeneric<TModifier>>) => Instance;
export { detectOverflow };
//# sourceMappingURL=index.d.ts.map