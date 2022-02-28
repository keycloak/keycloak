In case you need to nest dropzone components and prevent any drag events from the child propagate to the parent, it can easily be achieved by using `.stopPropagation()` in the child dropzone.

```jsx harmony
const parentStyle = {
  width: 200,
  height: 200,
  border: '2px dashed #888'
}

const childStyle = {
  width: 160,
  height: 160,
  margin: 20,
  border: '2px dashed #ccc'
}

class NestedDropzone extends React.Component {
  constructor() {
    super()
    this.state = {
      parent: {},
      child: {}
    }
  }

  createDragHandler(eventType, node) {
    const updater = this.createStateUpdater(eventType, node)
    return (evt) => {
      evt.preventDefault();
      if (node === 'child') {
        evt.stopPropagation()
      }
      this.setState(updater)
    }
  }

  createDropHandler(node) {
    const updater = this.createStateUpdater('drop', node)
    return (accepted, rejected, evt) => {
      evt.preventDefault();
      if (node === 'child') {
        evt.stopPropagation()
      }
      this.setState(updater)
    }
  }

  createStateUpdater(eventType, node) {
    return state => {
        const events = {...state[node]};
        if (eventType !== events.current) {
          events.previous = events.current;
        }
        events.current = eventType;
        return {
          [node]: events
        }
      }
  }

  render() {
    return (
      <section>
        <div className="dropzone">
          <Dropzone
            onDragStart={this.createDragHandler('dragstart', 'parent')}
            onDragEnter={this.createDragHandler('dragenter', 'parent')}
            onDragOver={this.createDragHandler('dragover', 'parent')}
            onDragLeave={this.createDragHandler('dragleave', 'parent')}
            onDrop={this.createDropHandler('parent')}
          >
            {({getRootProps, getInputProps}) => (
              <div {...getRootProps()} style={parentStyle}>
                <Dropzone
                  onDragStart={this.createDragHandler('dragstart', 'child')}
                  onDragEnter={this.createDragHandler('dragenter', 'child')}
                  onDragOver={this.createDragHandler('dragover', 'child')}
                  onDragLeave={this.createDragHandler('dragleave', 'child')}
                  onDrop={this.createDropHandler('child')}
                  style={childStyle}
                >
                {({getRootProps, getInputProps}) => (
                  <div {...getRootProps()} style={childStyle} />
                )}
                </Dropzone>
              </div>
            )}
          </Dropzone>
        </div>
        <aside>
          <p>Parent: {JSON.stringify(this.state.parent)}</p>
          <p>Child: {JSON.stringify(this.state.child)}</p>
        </aside>
      </section>
    );
  }
}

<NestedDropzone />
```
