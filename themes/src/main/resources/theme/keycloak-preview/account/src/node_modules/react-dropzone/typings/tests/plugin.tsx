import React, {Component} from "react";
import Dropzone from "../../";

export class TestReactDragEvt extends Component {
  getFiles = async (evt: React.DragEvent<HTMLDivElement>) => {
    const files = Array.from(evt.dataTransfer.files);
    return files;
  }

  render() {
    return (
      <div>
        <Dropzone getDataTransferItems={this.getFiles}>
          {({getRootProps}) => (
            <div {...getRootProps()} />
          )}
        </Dropzone>
      </div>
    );
  }
}

export class TestDataTransferItems extends Component {
  getFiles = async (evt: React.DragEvent<HTMLDivElement>) => {
    const items = Array.from(evt.dataTransfer.items);
    return items;
  }

  render() {
    return (
      <div>
        <Dropzone getDataTransferItems={this.getFiles}>
          {({getRootProps}) => (
            <div {...getRootProps()} />
          )}
        </Dropzone>
      </div>
    );
  }
}

export class TestNativeDragEventEvt extends Component {
  getFiles = async (evt: DragEvent) => {
    const files = Array.from(evt.dataTransfer.files);
    return files;
  }

  render() {
    return (
      <div>
        <Dropzone getDataTransferItems={this.getFiles}>
          {({getRootProps}) => (
            <div {...getRootProps()} />
          )}
        </Dropzone>
      </div>
    );
  }
}

export class TestChangeEvt extends Component {
  getFiles = async (evt: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(evt.target.files);
    return files;
  }

  render() {
    return (
      <div>
        <Dropzone getDataTransferItems={this.getFiles}>
          {({getRootProps}) => (
            <div {...getRootProps()} />
          )}
        </Dropzone>
      </div>
    );
  }
}


export class TestNativeEvt extends Component {
  getFiles = async (evt: Event) => {
    const files = Array.from((evt.target as HTMLInputElement).files);
    return files;
  }

  render() {
    return (
      <div>
        <Dropzone getDataTransferItems={this.getFiles}>
          {({getRootProps}) => (
            <div {...getRootProps()} />
          )}
        </Dropzone>
      </div>
    );
  }
}
