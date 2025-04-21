If you'd like to prevent the default behaviour for `onClick`, `onFocus`, `onBlur` and `onKeyDown`, use the `preventDefault()` fn on the event:

```jsx harmony
class Events extends React.Component {
  render() {
    return (
      <Dropzone>
        {({getRootProps, getInputProps}) => (
          <div {...getRootProps({onClick: evt => evt.preventDefault()})}>
            <input {...getInputProps()} />
            <p>Click to select files should not work!</p>
          </div>
        )}
      </Dropzone>
    );
  }
}

<Events />
```
