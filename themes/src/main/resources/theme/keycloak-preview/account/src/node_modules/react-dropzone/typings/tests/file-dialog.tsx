import React from "react";
import Dropzone from "../../";

export const dropzone = (
  <Dropzone onDrop={files => console.log(files)}>
    {({getRootProps, getInputProps, open}) => (
      <div {...getRootProps()}>
        <input {...getInputProps()} />
        <p>Drop some files here.</p>
        <button type="button" onClick={open}>
          Open file dialog
        </button>
      </div>
    )}
  </Dropzone>
);
