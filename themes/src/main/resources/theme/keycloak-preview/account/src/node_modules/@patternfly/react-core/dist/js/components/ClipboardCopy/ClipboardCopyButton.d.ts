import * as React from 'react';
export interface ClipboardCopyButtonProps extends React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement> {
    onClick: (event: React.MouseEvent) => void;
    children: React.ReactNode;
    id: string;
    textId: string;
    className?: string;
    exitDelay?: number;
    entryDelay?: number;
    maxWidth?: string;
    position?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
    'aria-label'?: string;
}
export declare const ClipboardCopyButton: React.FunctionComponent<ClipboardCopyButtonProps>;
