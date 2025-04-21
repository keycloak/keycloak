You can wrap the whole app into the dropzone. This will make the whole app a Dropzone target.

```jsx harmony
const overlayStyle = {
  position: 'absolute',
  top: 0,
  right: 0,
  bottom: 0,
  left: 0,
  padding: '2.5em 0',
  background: 'rgba(0,0,0,0.5)',
  textAlign: 'center',
  color: '#fff'
};

class FullScreen extends React.Component {
  constructor() {
    super()
    this.state = {
      accept: '',
      files: []
    }
  }

  onDrop(files) {
    this.setState({files});
  }

  applyMimeTypes(event) {
    this.setState({
      accept: event.target.value
    });
  }

  render() {
    const { accept } = this.state;

    const files = this.state.files.map((file, index) => (
      <li key={file.name}>
        {file.name} - {file.size} bytes
      </li>
    ))

    return (
      <Dropzone
        accept={accept}
        onDrop={this.onDrop.bind(this)}
      >
        {({getRootProps, getInputProps, isDragActive}) => (
            <div {...getRootProps({onClick: evt => evt.preventDefault()})} style={{position: "relative"}}>
              <input {...getInputProps()} />
              { isDragActive && <div style={overlayStyle}>Drop files here</div> }
              <h4>My awesome app</h4>
              <label htmlFor="mimetypes">Enter mime types you want to accept: </label>
              <input
                type="text"
                id="mimetypes"
                onChange={this.applyMimeTypes.bind(this)}
              />

              <h4>Files</h4>
              <ul>{files}</ul>
            </div>
        )}
      </Dropzone>
    );
  }
}

<FullScreen />
```
