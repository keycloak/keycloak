import * as React from 'react';
export interface ClipboardCopyToggleProps extends React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement> {
    onClick: (event: React.MouseEvent) => void;
    id: string;
    textId: string;
    contentId: string;
    isExpanded?: boolean;
    className?: string;
}
export declare const ClipboardCopyToggle: React.FunctionComponent<ClipboardCopyToggleProps>;
