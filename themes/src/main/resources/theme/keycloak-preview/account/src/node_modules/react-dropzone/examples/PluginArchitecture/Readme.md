Dropzone accepts `getDataTransferItems` prop that enhances the handling of dropped file system objects and allows more flexible use of them e.g. passing a function that accepts drop event of a folder and resolves it to an array of files adds plug-in functionality of folders drag-and-drop.

Though, note that the provided `getDataTransferItems` fn must return a `Promise` with a list of `File` objects (or `DataTransferItem` of `{kind: 'file'}` when data cannot be accessed).
Otherwise, the results will be discarded and none of the drag events callbacks will be invoked.

In case you need to extend the `File` with some additional properties, you should use [Object.defineProperty()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/defineProperty) so that the result will still pass through our filter:

```jsx harmony
async function myCustomFileGetter(evt) {
  const files = Array.from(evt.dataTransfer.files);

  files.forEach(file => {
    Object.defineProperty(file, 'myProp', {
      value: true
    });
  })

  return files;
}

class Plugin extends React.Component {
  constructor() {
    super()
    this.state = {
      files: []
    }
  }

  onDrop(files) {
    this.setState({
      files
    })
  }

  render() {
    const files = this.state.files.map(f => (
      <li key={f.name}>
        {f.name} has <strong>myProps</strong>: {f.myProp === true ? 'YES' : ''}
      </li>
    ))
    return (
      <section>
        <div>
          <Dropzone
            getDataTransferItems={evt => myCustomFileGetter(evt)}
            onDrop={this.onDrop.bind(this)}
          >
            {({getRootProps, getInputProps}) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                  <p>Drop files here</p>
              </div>
            )}
          </Dropzone>
        </div>
        <aside>
          <h4>Files</h4>
          <ul>{files}</ul>
        </aside>
      </section>
    )
  }
}

<Plugin />
```
