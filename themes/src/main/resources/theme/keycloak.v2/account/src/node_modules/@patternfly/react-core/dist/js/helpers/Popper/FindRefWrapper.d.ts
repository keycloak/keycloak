import * as React from 'react';
interface FindRefWrapperProps {
    children: React.ReactNode;
    onFoundRef: any;
}
/**
 * This component wraps any ReactNode and finds its ref
 * It has to be a class for findDOMNode to work
 * Ideally, all components used as triggers/toggles are either:
 * - class based components we can assign our own ref to
 * - functional components that have forwardRef implemented
 * However, there is no guarantee that is what will get passed in as trigger/toggle in the case of tooltips and popovers
 */
export declare class FindRefWrapper extends React.Component<FindRefWrapperProps> {
    static displayName: string;
    componentDidMount(): void;
    render(): {};
}
export {};
//# sourceMappingURL=FindRefWrapper.d.ts.map