---
title: File upload
section: components
cssPrefix: pf-c-file-upload
beta: true
---

## Examples

```hbs title=Basic-file-upload
{{#> file-upload file-upload--id="basic-file-upload"}}
  {{#> file-upload-file-select}}
    {{#> input-group}}
      {{> file-upload-text-input
        file-upload-text-input--aria-label="Drag a file here or browse to upload"
        file-upload-text-input--attribute=(concat 'readonly placeholder="Drag a file here or browse to upload" aria-describedby="' file-upload--id '-browse"')
        }}
      {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' file-upload--id '-browse"')}}
        Browse...
      {{/button}}
      {{#> button button--modifier="pf-m-control" button--attribute="disabled"}}
        Clear
      {{/button}}
    {{/input-group}}
  {{/file-upload-file-select}}
  {{#> file-upload-file-details file-upload-file-details--aria-label="Empty text area"}}
  {{/file-upload-file-details}}
{{/file-upload}}
```

```hbs title=Upload-complete-non-editable
{{#> file-upload file-upload--id="browsed-file-upload-complete"}}
  {{#> file-upload-file-select}}
    {{#> input-group}}
      {{> file-upload-text-input
        file-upload-text-input--aria-label="Read only filename"
        file-upload-text-input--attribute=(concat 'readonly value="Read only filename" aria-describedby="' file-upload--id '-browse"')
        }}
      {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' file-upload--id '-browse"')}}
        Browse...
      {{/button}}
      {{#> button button--modifier="pf-m-control"}}
        Clear
      {{/button}}
    {{/input-group}}
  {{/file-upload-file-select}}
  {{#> file-upload-file-details file-upload-file-details--aria-label="Text area" file-upload-file-details--attribute='readonly'}}Ssh-Rsa AAh3zJFkzjjakCJialksjfB3zJFkzzAAhhMskjjakCJialksjfB3z89z3zJFkz3 +kzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fc
  {{/file-upload-file-details}}
{{/file-upload}}
```

```hbs title=Upload-complete-editable
{{#> file-upload file-upload--id="drop-file"}}
  {{#> file-upload-file-select}}
    {{#> input-group}}
      {{> file-upload-text-input
        file-upload-text-input--aria-label="Read only filename"
        file-upload-text-input--attribute=(concat 'readonly value="Sample.txt" aria-describedby="' file-upload--id '-browse"')
        }}
      {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' file-upload--id '-browse"')}}
        Browse...
      {{/button}}
      {{#> button button--modifier="pf-m-control"}}
        Clear
      {{/button}}
    {{/input-group}}
  {{/file-upload-file-select}}
  {{#> file-upload-file-details file-upload-file-details--aria-label="Text area"}}Ssh-Rsa AAh3zJFkzjjakCJialksjfB3zJFkzzAAhhMskjjakCJialksjfB3z89z3zJFkz3 +kzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fc
  {{/file-upload-file-details}}
{{/file-upload}}
```

```hbs title=Drag-file-hover-component
{{#> file-upload file-upload--id="drag-file-hover-component" file-upload--modifier="pf-m-drag-hover"}}
  {{#> file-upload-file-select}}
    {{#> input-group}}
      {{> file-upload-text-input file-upload-text-input--aria-label="Drag a file here or browse to upload" file-upload-text-input--attribute=(concat 'readonly placeholder="Drag a file here or browse to upload" aria-describedby="' file-upload--id '-browse"')}}
      {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' file-upload--id '-browse"')}}
        Browse...
      {{/button}}
      {{#> button button--modifier="pf-m-control" button--attribute="disabled"}}
        Clear
      {{/button}}
    {{/input-group}}
  {{/file-upload-file-select}}
  {{#> file-upload-file-details file-upload-file-details--aria-label="Empty text area"}}
  {{/file-upload-file-details}}
{{/file-upload}}
```

```hbs title=File-upload-in-form-with-error
{{#> form}}
  {{#> form-group}}
    {{#> file-upload file-upload--id="file-upload-error"}}
      {{#> file-upload-file-select}}
        {{#> input-group}}
          {{> file-upload-text-input
            file-upload-text-input--aria-label="File upload error"
            file-upload-text-input--attribute=(concat 'required value="Sample.png"  aria-describedby="' file-upload--id '-browse"')
            }}
          {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' file-upload--id '-browse"')}}
            Browse...
          {{/button}}
          {{#> button button--modifier="pf-m-control"}}
            Clear
          {{/button}}
        {{/input-group}}
      {{/file-upload-file-select}}
      {{#> file-upload-file-details file-upload-file-details--attribute='aria-describedby="textAreaHelperText1" aria-invalid="true"' file-upload-file-details--aria-label="Empty text area"}}{{/file-upload-file-details}}
    {{/file-upload}}
    {{#> form-helper-text form-helper-text--modifier="pf-m-error" form-helper-text--attribute='id="textAreaHelperText1" aria-live="polite"'}}
      We don't support this file type. Try again with a different file type.
    {{/form-helper-text}}
  {{/form-group}}
{{/form}}
```

```hbs title=File-upload-loading
{{#> file-upload file-upload--id="file-upload-loading" file-upload--modifier="pf-m-loading"}}
  {{#> file-upload-file-select}}
    {{#> input-group}}
      {{> file-upload-text-input
        file-upload-text-input--aria-label="Read only filename"
        file-upload-text-input--attribute=(concat 'readonly name="file-upload-loading" value="Sample.png" aria-describedby="' file-upload--id '-browse"')
      }}
      {{#> button button--modifier="pf-m-control" button--attribute=(concat 'disabled id="' file-upload--id '-browse"')}}
        Browse...
      {{/button}}
      {{#> button button--modifier="pf-m-control"}}
        Clear
      {{/button}}
    {{/input-group}}
  {{/file-upload-file-select}}
  {{#> file-upload-file-details file-upload-file-details--aria-label="Text area" file-upload-file-details--HasSpinner="true"}}Ssh-Rsa AAh3zJFkzjjakCJialksjfB3zJFkzzAAhhMskjjakCJialksjfB3z89z3zJFkz3 +kzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fczJFkzMAjsauoox88aaZXphBx4fc
  {{/file-upload-file-details}}
{{/file-upload}}
```

## Documentation

### Overview

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-file-upload` | `<div>`, `<form>` | Initiates the file upload component. **Required**. |
| `.pf-c-file-upload__file-select` | `<div>` | Initiates the file select element. **Required** |
| `.pf-c-file-upload__file-details` | `<div>` | Initiates the file details element. **Required** |
| `.pf-c-file-upload__file-details-spinner` | `<div>` | Initiates the file details element. **Required** |
| `.pf-m-drag-hover` | `.pf-c-file-upload` | Modifies file upload for when an element is dragged or dropped inside of its container. |
| `.pf-m-loading` | `.pf-c-file-upload` | Modifies file upload for the loading state. |
