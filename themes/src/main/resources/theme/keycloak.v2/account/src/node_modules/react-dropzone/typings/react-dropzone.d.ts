import * as React from "react";
// import {func} from "prop-types";

export default class Dropzone extends React.Component<DropzoneProps> {
  open: () => void;
}

export type DropzoneProps = Pick<React.HTMLProps<HTMLElement>, PropTypes> & {
  children?: DropzoneRenderFunction;
  getDataTransferItems?(event: React.DragEvent<HTMLElement> | React.ChangeEvent<HTMLInputElement> | DragEvent | Event): Promise<Array<File | DataTransferItem>>;
  onFileDialogCancel?(): void;
  onDrop?: DropFilesEventHandler;
  onDropAccepted?: DropFileEventHandler;
  onDropRejected?: DropFileEventHandler;
  maxSize?: number;
  minSize?: number;
  preventDropOnDocument?: boolean;
  disabled?: boolean;
};

export interface DropzoneRootProps extends React.HTMLAttributes<HTMLElement> {
  refKey?: string;
  [key: string]: any;
}

export interface DropzoneInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  refKey?: string;
}

export type DropzoneRenderFunction = (x: DropzoneRenderArgs) => JSX.Element;
export type GetRootPropsFn = (props?: DropzoneRootProps) => DropzoneRootProps;
export type GetInputPropsFn = (props?: DropzoneInputProps) => DropzoneInputProps;

export type DropFileEventHandler = (
  acceptedOrRejected: File[],
  event: React.DragEvent<HTMLElement>
) => void;

export type DropFilesEventHandler = (
  accepted: File[],
  rejected: File[],
  event: React.DragEvent<HTMLElement>
) => void;

export type DropzoneRenderArgs = {
  draggedFiles: File[];
  acceptedFiles: File[];
  rejectedFiles: File[];
  isDragActive: boolean;
  isDragAccept: boolean;
  isDragReject: boolean;
  getRootProps: GetRootPropsFn;
  getInputProps: GetInputPropsFn;
  open: () => void;
};

type PropTypes = "accept"
  | "multiple"
  | "name"
  | "onClick"
  | "onFocus"
  | "onBlur"
  | "onKeyDown"
  | "onDragStart"
  | "onDragEnter"
  | "onDragOver"
  | "onDragLeave";
