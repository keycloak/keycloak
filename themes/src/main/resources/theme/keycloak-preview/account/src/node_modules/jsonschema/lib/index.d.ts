/*
This is type definition for typescript.
This is for library users. Thus, properties and methods for internal use is omitted.
 */
export declare class Validator {
    constructor();
    customFormats: {[formatName: string]: CustomFormat};
    schemas: {[id: string]: Schema};
    unresolvedRefs: string[];

    attributes: {[property: string]: CustomProperty};

    addSchema(schema?: Schema, uri?: string): Schema|void;
    validate(instance: any, schema: Schema, options?: Options, ctx?: SchemaContext): ValidatorResult;
}

export declare class ValidatorResult {
    constructor(instance: any, schema: Schema, options: Options, ctx: SchemaContext)
    instance: any;
    schema: Schema;
    propertyPath: string;
    errors: ValidationError[];
    throwError: boolean;
    disableFormat: boolean;
    valid: boolean;
    addError(detail: string|ErrorDetail): ValidationError;
    toString(): string;
}

export declare class ValidationError {
    constructor(message?: string, instance?: any, schema?: Schema, propertyPath?: any, name?: string, argument?: any);
    property: string;
    message: string;
    schema: string|Schema;
    instance: any;
    name: string;
    argument: any;
    toString(): string;
    stack: string;
}

export declare class SchemaError extends Error{
    constructor(msg: string, schema: Schema);
    schema: Schema;
    message: string;
}

export declare function validate(instance: any, schema: any, options?: Options): ValidatorResult

export interface Schema {
    id?: string
    $schema?: string
    $ref?: string
    title?: string
    description?: string
    multipleOf?: number
    maximum?: number
    exclusiveMaximum?: boolean
    minimum?: number
    exclusiveMinimum?: boolean
    maxLength?: number
    minLength?: number
    pattern?: string | RegExp
    additionalItems?: boolean | Schema
    items?: Schema | Schema[]
    maxItems?: number
    minItems?: number
    uniqueItems?: boolean
    maxProperties?: number
    minProperties?: number
    required?: string[] | boolean
    additionalProperties?: boolean | Schema
    definitions?: {
        [name: string]: Schema
    }
    properties?: {
        [name: string]: Schema
    }
    patternProperties?: {
        [name: string]: Schema
    }
    dependencies?: {
        [name: string]: Schema | string[]
    }
    'enum'?: any[]
    type?: string | string[]
    format?: string
    allOf?: Schema[]
    anyOf?: Schema[]
    oneOf?: Schema[]
    not?: Schema
}

export interface Options {
    skipAttributes?: string[];
    allowUnknownAttributes?: boolean;
    rewrite?: RewriteFunction;
    propertyName?: string;
    base?: string;
    throwError?: boolean;
}

export interface RewriteFunction {
    (instance: any, schema: Schema, options: Options, ctx: SchemaContext): any;
}

export interface SchemaContext {
    schema: Schema;
    options: Options;
    propertyPath: string;
    base: string;
    schemas: {[base: string]: Schema};
}

export interface CustomFormat {
    (input: any): boolean;
}

export interface CustomProperty {
    (instance: any, schema: Schema, options: Options, ctx: SchemaContext): string|ValidatorResult;
}

export interface ErrorDetail {
    message: string;
    name: string;
    argument: string;
}
