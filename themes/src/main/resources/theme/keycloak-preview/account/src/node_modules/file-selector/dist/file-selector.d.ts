import { FileWithPath } from './file';
/**
 * Convert a DragEvent's DataTrasfer object to a list of File objects
 * NOTE: If some of the items are folders,
 * everything will be flattened and placed in the same list but the paths will be kept as a {path} property.
 * @param evt
 */
export declare function fromEvent(evt: Event): Promise<Array<FileWithPath | DataTransferItem>>;
