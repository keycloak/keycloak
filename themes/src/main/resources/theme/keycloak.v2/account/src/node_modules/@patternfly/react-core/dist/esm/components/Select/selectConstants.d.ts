import * as React from 'react';
import { SelectOptionObject } from './SelectOption';
export interface SelectContextInterface {
    onSelect: (event: React.MouseEvent<any, MouseEvent> | React.ChangeEvent<HTMLInputElement>, value: string | SelectOptionObject, isPlaceholder?: boolean) => void;
    onClose: () => void;
    onFavorite: (itemId: string, isFavorite: boolean) => void;
    variant: string;
    inputIdPrefix: string;
    shouldResetOnSelect: boolean;
}
export declare const SelectContext: React.Context<SelectContextInterface>;
export declare const SelectProvider: React.Provider<SelectContextInterface>;
export declare const SelectConsumer: React.Consumer<SelectContextInterface>;
export declare enum SelectVariant {
    single = "single",
    checkbox = "checkbox",
    typeahead = "typeahead",
    typeaheadMulti = "typeaheadmulti"
}
export declare enum SelectPosition {
    right = "right",
    left = "left"
}
export declare enum SelectDirection {
    up = "up",
    down = "down"
}
export declare const SelectFooterTabbableItems = "input, button, select, textarea, a[href]";
//# sourceMappingURL=selectConstants.d.ts.map