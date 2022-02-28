By default, the Dropzone component doesn't render any styles.
By providing a function that returns the component's children you can not only style Dropzone appropriately but also render appropriate content.

### Using inline styles

By providing a function that returns the component's children you can not only style Dropzone appropriately but also render appropriate content.

```jsx harmony
const baseStyle = {
  width: 200,
  height: 200,
  borderWidth: 2,
  borderColor: '#666',
  borderStyle: 'dashed',
  borderRadius: 5
};
const activeStyle = {
  borderStyle: 'solid',
  borderColor: '#6c6',
  backgroundColor: '#eee'
};
const rejectStyle = {
  borderStyle: 'solid',
  borderColor: '#c66',
  backgroundColor: '#eee'
};

<Dropzone accept="image/*">
  {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject, acceptedFiles, rejectedFiles }) => {
    let styles = {...baseStyle}
    styles = isDragActive ? {...styles, ...activeStyle} : styles
    styles = isDragReject ? {...styles, ...rejectStyle} : styles

    return (
      <div
        {...getRootProps()}
        style={styles}
      >
        <input {...getInputProps()} />
        <div>
          {isDragAccept ? 'Drop' : 'Drag'} files here...
        </div>
        {isDragReject && <div>Unsupported file type...</div>}
      </div>
    )
  }}
</Dropzone>
```

### Using styled-components

```jsx harmony
const styled = require('styled-components').default;

const getColor = (props) => {
  if (props.isDragReject) {
      return '#c66';
  }
  if (props.isDragActive) {
      return '#6c6';
  }
  return '#666';
};

const Container = styled.div`
  width: 200px;
  height: 200px;
  border-width: 2px;
  border-radius: 5px;
  border-color: ${props => getColor(props)};
  border-style: ${props => props.isDragReject || props.isDragActive ? 'solid' : 'dashed'};
  background-color: ${props => props.isDragReject || props.isDragActive ? '#eee' : ''};
`;

<Dropzone accept="image/*">
  {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject, acceptedFiles }) => {
    return (
      <Container
        isDragActive={isDragActive}
        isDragReject={isDragReject}
        {...getRootProps()}
      >
        <input {...getInputProps()} />
        {isDragAccept ? 'Drop' : 'Drag'} files here...
      </Container>
    )
  }}
</Dropzone>
```
