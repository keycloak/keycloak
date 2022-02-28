export interface StyleDeclarationStatic {
  __className: string;
  __inject(): void;
}

export function isValidStyleDeclaration(obj: any): obj is StyleDeclarationStatic;

export function createStyleDeclaration(className: string, rawCss: string): StyleDeclarationStatic;

export function isModifier(className: string): boolean;

export function getModifier(
  styleObject: any,
  modifier: string,
  defaultModifer?: StyleDeclarationStatic | string
): string;

export function formatClassName(className: string): string;

export function getCSSClasses(cssString: string): string[];

export function getInsertedStyles(): string[];

export function getClassName(obj: StyleDeclarationStatic | string): string;

export function pickProperties(object: any, properties: string[]): any;
