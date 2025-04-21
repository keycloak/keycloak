import * as React from 'react';
export interface ClipboardCopyToggleProps extends Omit<React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement>, 'ref'> {
    onClick: (event: React.MouseEvent) => void;
    id: string;
    textId: string;
    contentId: string;
    isExpanded?: boolean;
    className?: string;
}
export declare const ClipboardCopyToggle: React.FunctionComponent<ClipboardCopyToggleProps>;
//# sourceMappingURL=ClipboardCopyToggle.d.ts.map