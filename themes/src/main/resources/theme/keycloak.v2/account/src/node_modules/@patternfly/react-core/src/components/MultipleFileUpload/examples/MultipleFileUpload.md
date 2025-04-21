---
id: File upload - multiple
section: components
cssPrefix: pf-c-multiple-file-upload
propComponents:
  [
    'MultipleFileUpload',
    'MultipleFileUploadMain',
    'MultipleFileUploadStatus',
    'MultipleFileUploadStatusItem',
  ]
beta: true
---

import UploadIcon from '@patternfly/react-icons/dist/esm/icons/upload-icon';

File upload - multiple is able to:

- Accept one or more files via browse or drag-and-drop
- Provide their data to you using file objects via the `onFileDrop` callback prop
- Read files as dataURLs, calling provided callbacks as needed when files start/finish being read, as well as when the read succeeds or fails
- Display (in real time) the upload progress/status of each file
  - Uploaded files are represented by the multiple file upload status item component, this component includes the aforementioned built-in file reading logic
  - If you prefer to supply your own file reading logic and strictly use multiple file upload status item as a display component, the `customFileHandler`, `fileName`, `fileSize`, `progressValue`, and `progressVariant` props exist to allow you to do that

### Restricting file size and type

As with singular file upload, any [props accepted by react-dropzone's Dropzone component](https://react-dropzone.js.org/#!/Dropzone) can be passed as a dropzoneProps object in order to customize the behavior of the Dropzone, such as restricting the type of files allowed. The following examples will only accept the files they list as accepted. Note that file type determination is not reliable across platforms (see the note on react-dropzone's docs about the accept prop), so be sure to test the behavior of your file upload restriction on all browsers and operating systems targeted by your application.

#### IMPORTANT: A note about security

Restricting file sizes and types in this way is for user convenience only, and it cannot prevent a malicious user from submitting anything to your server. As with any user input, your application should also validate, sanitize and/or reject restricted files on the server side.

## Examples

### Basic

```ts file="./MultipleFileUploadBasic.tsx"
```
