import * as React from 'react';
export declare const OuiaContext: React.Context<OuiaContextProps>;
export interface InjectedOuiaProps {
    ouiaContext?: OuiaContextProps;
    ouiaId?: number | string;
}
export interface OuiaContextProps {
    isOuia?: boolean;
    ouiaId?: number | string;
}
/**
 * @param { React.ComponentClass | React.FunctionComponent } WrappedComponent - React component
 */
export declare function withOuiaContext<P extends {
    ouiaContext?: OuiaContextProps;
}>(WrappedComponent: React.ComponentClass<P> | React.FunctionComponent<P>): React.FunctionComponent<P>;
