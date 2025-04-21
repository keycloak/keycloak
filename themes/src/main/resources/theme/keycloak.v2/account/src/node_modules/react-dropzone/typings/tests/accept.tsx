import React from "react";
import Dropzone from "../../";

export default class Accept extends React.Component {
  state = {
    accepted: [],
    rejected: []
  };

  render() {
    return (
      <section>
        <div className="dropzone">
          <Dropzone
            accept="image/jpeg, image/png"
            onDrop={(accepted, rejected) => {
              this.setState({ accepted, rejected });
            }}
          >
            {({getRootProps}) => (
              <div {...getRootProps()}>
                <p>
                  Try dropping some files here, or click to select files to upload.
                </p>
                <p>Only *.jpeg and *.png images will be accepted</p>
              </div>
            )}
          </Dropzone>
        </div>
        <aside>
          <h2>Accepted files</h2>
          <ul>
            {this.state.accepted.map(f => (
              <li key={f.name}>
                {f.name} - {f.size} bytes
              </li>
            ))}
          </ul>
          <h2>Rejected files</h2>
          <ul>
            {this.state.rejected.map(f => (
              <li key={f.name}>
                {f.name} - {f.size} bytes
              </li>
            ))}
          </ul>
        </aside>
      </section>
    );
  }
}

export const acceptExt = (
  <Dropzone accept=".jpeg,.png">
    {({getRootProps, isDragActive, isDragAccept, isDragReject}) => (
      <div {...getRootProps()}>
        {isDragAccept && "All files will be accepted"}
        {isDragReject && "Some files will be rejected"}
        {isDragActive && "Drop some files here ..."}
      </div>
    )}
  </Dropzone>
);

export const acceptMime = (
  <Dropzone accept="image/jpeg, image/png">
    {({getRootProps, isDragActive, isDragAccept, isDragReject}) => (
      <div {...getRootProps()}>
        {isDragAccept && "All files will be accepted"}
        {isDragReject && "Some files will be rejected"}
        {isDragActive && "Drop some files here ..."}
      </div>
    )}
  </Dropzone>
);
