By default, Dropzone just renders provided children without applying any styles.

Furthermore, Dropzone supports folders drag 'n' drop. See [file-selector](https://github.com/react-dropzone/file-selector) for more info about supported browsers.


```jsx harmony
class Basic extends React.Component {
  constructor() {
    super()
    this.state = {
      files: []
    }
  }

  onDrop(files) {
    this.setState({files});
  }

  onCancel() {
    this.setState({
      files: []
    });
  }

  render() {
    const files = this.state.files.map(file => (
      <li key={file.name}>
        {file.name} - {file.size} bytes
      </li>
    ))

    return (
      <section>
        <Dropzone
          onDrop={this.onDrop.bind(this)}
          onFileDialogCancel={this.onCancel.bind(this)}
        >
          {({getRootProps, getInputProps}) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
                <p>Drop files here, or click to select files</p>
            </div>
          )}
        </Dropzone>
        <aside>
          <h4>Files</h4>
          <ul>{files}</ul>
        </aside>
      </section>
    );
  }
}

<Basic />
```

Dropzone with `disabled` property:

```jsx harmony
class Basic extends React.Component {
  constructor() {
    super()
    this.state = {
      disabled: true,
      files: []
    }
  }

  onDrop(files) {
    this.setState({files});
  }

  toggleDisabled() {
    this.setState({
      disabled: !this.state.disabled
    })
  }

  render() {
    const files = this.state.files.map(file => (
      <li key={file.name}>
        {file.name} - {file.size} bytes
      </li>
    ))

    return (
      <section>
        <aside>
          <button
            type="button"
            onClick={this.toggleDisabled.bind(this)}
          >
            Toggle disabled
          </button>
        </aside>
        <div className="dropzone">
          <Dropzone
            onDrop={this.onDrop.bind(this)}
            disabled={this.state.disabled}
          >
            {({getRootProps, getInputProps}) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                 <p>Drop files here, or click to select files</p>
              </div>
            )}
          </Dropzone>
        </div>
        <aside>
          <h4>Files</h4>
          <ul>{files}</ul>
        </aside>
      </section>
    );
  }
}

<Basic />
```
