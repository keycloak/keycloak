---
id: File upload
cssPrefix: pf-c-file-upload
propComponents: ['FileUpload', 'FileUploadField']
section: components
---

import FileUploadIcon from '@patternfly/react-icons/dist/esm/icons/file-upload-icon';

## Examples

The basic `FileUpload` component can accept a file via browse or drag-and-drop, and behaves like a standard form field with its `value` and `onFileInputChange` event that is similar to `<input onChange="...">` prop. The `type` prop determines how the `FileUpload` component behaves upon accepting a file, what type of value it passes to its `onDataChange` event.

### Text files

If `type="text"` is passed (and `hideDefaultPreview` is not), a `TextArea` preview will be rendered underneath the filename bar. When a file is selected, its contents will be read into memory and passed to the `onDataChange` event as a string. Every filename change is passed to `onFileInputChange` same as it would do with the `<input>` element.
Pressing _Clear_ button triggers `onClearClick` event.

### Simple text file

```ts file="./FileUploadSimpleText.tsx"
```

A user can always type instead of selecting a file, but by default, once a user selects a text file from their disk they are not allowed to edit it (to prevent unintended changes to a format-sensitive file). This behavior can be changed with the `allowEditingUploadedText` prop.
Typing/pasting text in the box will call `onTextChange` with a string, and a string value is expected for the `value` prop. :

### Text file with edits allowed

```ts file="./FileUploadTextWithEdits.tsx"
```

### Restricting file size and type

Any [props accepted by `react-dropzone`'s `Dropzone` component](https://react-dropzone.js.org/#!/Dropzone) can be passed as a `dropzoneProps` object in order to customize the behavior of the Dropzone, such as restricting the size and type of files allowed. The following example will only accept CSV files smaller than 1 KB. Note that file type determination is not reliable across platforms (see the note on react-dropzone's docs about the `accept` prop), so be sure to test the behavior of your file upload restriction on all browsers and operating systems targeted by your application.

#### IMPORTANT: A note about security

Restricting file sizes and types in this way is for user convenience only, and it cannot prevent a malicious user from submitting anything to your server. As with any user input, your application should also validate, sanitize and/or reject restricted files on the server side.

### Text file with restrictions

```ts file="./FileUploadTextWithRestrictions.tsx"
```

### Other file types

If no `type` prop is specified, the component will not read files directly. When a file is selected, a [`File` object](https://developer.mozilla.org/en-US/docs/Web/API/File) will be passed as a second argument to `onFileInputChange` and your application will be responsible for reading from it (e.g. by using the [FileReader API](https://developer.mozilla.org/en-US/docs/Web/API/FileReader) or attaching it to a [FormData object](https://developer.mozilla.org/en-US/docs/Web/API/FormData/Using_FormData_Objects)). A `File` object will also be expected for the `value` prop instead of a string, and no preview of the file contents will be shown by default. The `onReadStarted` and `onReadFinished` callbacks will also not be called since the component is not reading the file.

### Simple file of any format

```ts file="./FileUploadSimpleFile.tsx"
```

### Customizing the file preview

Regardless of `type`, the preview area (the TextArea, or any future implementations of default previews for other types) can be removed by passing `hideDefaultPreview`, and a custom one can be rendered by passing `children`.

### Custom file preview

```ts file="./FileUploadCustomPreview.tsx"
```

### Bringing your own file browse logic

`FileUpload` is a thin wrapper around the `FileUploadField` presentational component. If you need to implement your own logic for accepting files, you can instead render a `FileUploadField` directly, which does not include `react-dropzone` and requires additional props (e.g. `onBrowseButtonClick`, `onClearButtonClick`, `isDragActive`).

Note that the `isLoading` prop is styled to position the spinner dead center above the entire component, so it should not be used with `hideDefaultPreview` unless a custom empty-state preview is provided via `children`. The below example prevents `isLoading` and `hideDefaultPreview` from being used at the same time. You can always provide your own spinner as part of the `children`!

### Custom file upload

```ts file="./FileUploadCustomUpload.tsx"
```
