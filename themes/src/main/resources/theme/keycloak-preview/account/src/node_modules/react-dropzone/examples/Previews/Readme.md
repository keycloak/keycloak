Starting with version 7.0.0, the `{preview}` property generation on the [File](https://developer.mozilla.org/en-US/docs/Web/API/File) objects and the `{disablePreview}` property on the `<Dropzone>` have been removed.

If you need need the `{preview}`, it can be easily achieved in the `onDrop()` callback:

```jsx harmony
const thumbsContainer = {
  display: 'flex',
  flexDirection: 'row',
  flexWrap: 'wrap',
  marginTop: 16
};

const thumb = {
  display: 'inline-flex',
  borderRadius: 2,
  border: '1px solid #eaeaea',
  marginBottom: 8,
  marginRight: 8,
  width: 100,
  height: 100,
  padding: 4,
  boxSizing: 'border-box'
};

const thumbInner = {
  display: 'flex',
  minWidth: 0,
  overflow: 'hidden'
}

const img = {
  display: 'block',
  width: 'auto',
  height: '100%'
};

class DropzoneWithPreview extends React.Component {
  constructor() {
    super()
    this.state = {
      files: []
    };
  }

  onDrop(files) {
    this.setState({
      files: files.map(file => Object.assign(file, {
        preview: URL.createObjectURL(file)
      }))
    });
  }

  componentWillUnmount() {
    // Make sure to revoke the data uris to avoid memory leaks
    this.state.files.forEach(file => URL.revokeObjectURL(file.preview))
  }

  render() {
    const {files} = this.state;

    const thumbs = files.map(file => (
      <div style={thumb} key={file.name}>
        <div style={thumbInner}>
          <img
            src={file.preview}
            style={img}
          />
        </div>
      </div>
    ));

    return (
      <section>
        <Dropzone
          accept="image/*"
          onDrop={this.onDrop.bind(this)}
        >
          {({getRootProps, getInputProps}) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Drop files here</p>
            </div>
          )}
        </Dropzone>
        <aside style={thumbsContainer}>
          {thumbs}
        </aside>
      </section>
    );
  }
}

<DropzoneWithPreview />
```
