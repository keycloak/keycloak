import React from "react";
import Dropzone from "../../";

export class Events extends React.Component {
  render() {
    return (
      <section>
        <div className="dropzone">
          <Dropzone
            onDrop={(acceptedFiles, rejectedFiles, event) =>
              console.log(acceptedFiles, rejectedFiles, event)}
            onDragStart={event => console.log(event)}
            onDragEnter={event => console.log(event)}
            onDragOver={event => console.log(event)}
            onDragLeave={event => console.log(event)}
          >
            {({getRootProps, getInputProps}) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                <p>
                  Try dropping some files here, or click to select files to upload.
                </p>
              </div>
            )}
          </Dropzone>
        </div>
      </section>
    );
  }
}
