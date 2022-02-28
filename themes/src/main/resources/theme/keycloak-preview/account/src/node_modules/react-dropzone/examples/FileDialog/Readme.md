You can programmatically invoke the default OS file prompt; there are two ways to do that:

- Provide a function as the child for `Dropzone` to access the `open` method as a parameter
- Set the ref on your `Dropzone` instance and call the instance `open` method

**Note** that for security reasons most browsers require popups and dialogues to originate from a direct user interaction (i.e. click).

If you are calling `dropzoneRef.open()` asynchronously, there’s a good chance it’s going to be blocked by the browser. So if you are calling `dropzoneRef.open()` asynchronously, be sure there is no more than *1000ms* delay between user interaction and `dropzoneRef.open()` call.

Due to the lack of official docs on this (at least we haven’t found any. If you know one, feel free to open PR), there is no guarantee that **allowed delay duration** will not be changed in later browser versions. Since implementations may differ between different browsers, avoid calling open asynchronously if possible.

#### Open programmatically using the children function param

```jsx harmony
<Dropzone
  onDrop={files => alert(JSON.stringify(files.map(f => f.name)))}
>
  {({getRootProps, getInputProps, open}) => (
    <div {...getRootProps({onClick: evt => evt.preventDefault()})}>
      <input {...getInputProps()} />
        <p>Drop files here</p>

        <button type="button" onClick={() => open()}>
          Open File Dialog
        </button>
    </div>
  )}
</Dropzone>
```

#### Open programmatically using the Dropzone ref

```jsx harmony
const dropzoneRef = React.createRef();

<Dropzone
  ref={dropzoneRef}
  onDrop={files => { alert(JSON.stringify(files.map(f => f.name))) }}
>
  {({getRootProps, getInputProps}) => (
    <div {...getRootProps({onClick: evt => evt.preventDefault()})}>
      <input {...getInputProps()} />
        <p>Drop files here</p>

        <button type="button" onClick={() => dropzoneRef.current.open()}>
          Open File Dialog
        </button>
    </div>
  )}
</Dropzone>
```
