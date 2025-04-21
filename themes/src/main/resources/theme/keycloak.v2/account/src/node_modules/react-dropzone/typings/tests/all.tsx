import React from "react";
import Dropzone from "../../";

export default class Test extends React.Component {
  dz: Dropzone;

  open() {
    this.dz.open();
  }

  handleFileDialog = () => {};

  render() {
    return (
      <div>
        <Dropzone
          onDrop={(acceptedFiles, rejectedFiles, event) =>
            console.log(acceptedFiles, rejectedFiles, event)}
          onDragStart={event => console.log(event)}
          onDragEnter={event => console.log(event)}
          onDragOver={event => console.log(event)}
          onDragLeave={event => console.log(event)}
          onDropAccepted={event => console.log(event)}
          onDropRejected={event => console.log(event)}
          onFileDialogCancel={() => console.log("abc")}
          minSize={2000}
          maxSize={Infinity}
          preventDropOnDocument
          disabled
          multiple={false}
          accept="*.png"
          name="dropzone"
        >
          {({getRootProps, getInputProps}) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      </div>
    );
  }
}
