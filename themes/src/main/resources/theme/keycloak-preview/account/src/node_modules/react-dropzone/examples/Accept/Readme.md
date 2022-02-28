By providing `accept` prop you can make Dropzone to accept specific file types and reject the others.

The value must be a comma-separated list of unique content type specifiers:
* A file extension starting with the STOP character (U+002E). (e.g. .jpg, .png, .doc).
* A valid MIME type with no extensions.
* audio/* representing sound files.
* video/* representing video files.
* image/* representing image files.

For more information see https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Input

```jsx harmony
class Accept extends React.Component {
  constructor() {
    super()
    this.state = {
      accepted: [],
      rejected: []
    }
  }

  render() {
    return (
      <section>
        <div className="dropzone">
          <Dropzone
            accept="image/jpeg, image/png"
            onDrop={(accepted, rejected) => { this.setState({ accepted, rejected }); }}
          >
            {({ getRootProps, getInputProps }) => (
              <div {...getRootProps()}  className="dropzone">
                <input {...getInputProps()} />
                <p>Try dropping some files here, or click to select files to upload.</p>
                <p>Only *.jpeg and *.png images will be accepted</p>
              </div>
            )}
          </Dropzone>
        </div>
        <aside>
          <h4>Accepted files</h4>
          <ul>
            {
              this.state.accepted.map(f => <li key={f.name}>{f.name} - {f.size} bytes</li>)
            }
          </ul>
          <h4>Rejected files</h4>
          <ul>
            {
              this.state.rejected.map(f => <li key={f.name}>{f.name} - {f.size} bytes</li>)
            }
          </ul>
        </aside>
      </section>
    );
  }
}

<Accept />
```

### Browser limitations

Because of HTML5 File API differences across different browsers during the drag, Dropzone will only display `rejected` styles in Chrome (and Chromium based browser). It isn't working in Safari nor IE.

Also, at this moment it's not possible to read file names (and thus, file extensions) during the drag operation. For that reason, if you want to react on different file types _during_ the drag operation, _you have to use_ mime types and not extensions! For example, the following example won't work even in Chrome:

```jsx harmony
<Dropzone accept=".jpeg,.png">
  {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject }) => (
    <div {...getRootProps()} className="dropzone">
      <input {...getInputProps()} />
      <div>
        {isDragAccept && "All files will be accepted"}
        {isDragReject && "Some files will be rejected"}
        {!isDragActive && "Drop some files here..."}
      </div>
    </div>
  )}
</Dropzone>
```

but this one will:

```jsx harmony
<Dropzone accept="image/jpeg, image/png">
  {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject }) => (
    <div {...getRootProps()} className="dropzone">
      <input {...getInputProps()} />
      <div>
        {isDragAccept && "All files will be accepted"}
        {isDragReject && "Some files will be rejected"}
        {!isDragActive && "Drop some files here..."}
      </div>
    </div>
  )}
</Dropzone>
```

### Notes

Mime type determination is not reliable accross platforms. CSV files, for example, are reported as text/plain under macOS but as application/vnd.ms-excel under Windows. In some cases there might not be a mime type set at all.

