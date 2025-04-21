import React from "react";
import Dropzone from "../../";

export default class Basic extends React.Component {
  state = { files: [] };

  onDrop = files => {
    this.setState({
      files
    });
  }

  render() {
    return (
      <section>
        <div className="dropzone">
          <Dropzone onDrop={this.onDrop}>
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
        <aside>
          <h2>Dropped files</h2>
          <ul>
            {this.state.files.map(f => (
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

export const optional = (
  <Dropzone>
    {({getRootProps, getInputProps}) => (
      <div {...getRootProps()}>
        <input {...getInputProps()} />
      </div>
    )}
  </Dropzone>
)
