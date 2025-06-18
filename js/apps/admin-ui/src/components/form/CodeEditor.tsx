import CodeEditorComponent from "@uiw/react-textarea-code-editor";

type CodeEditorProps = {
  id?: string;
  "aria-label"?: string;
  "data-testid"?: string;
  value?: string;
  onChange?: (value: string) => void;
  language?: string;
  readOnly?: boolean;
  /* The height of the editor in pixels */
  height?: number;
};

const CodeEditor = ({ onChange, height = 128, ...rest }: CodeEditorProps) => (
  <div style={{ height: `${height}px`, overflow: "auto" }}>
    <CodeEditorComponent
      padding={15}
      minHeight={height}
      style={{
        font: "var(--pf-global--FontFamily--monospace)",
      }}
      onChange={(event) => onChange?.(event.target.value)}
      {...rest}
    />
  </div>
);

export default CodeEditor;
