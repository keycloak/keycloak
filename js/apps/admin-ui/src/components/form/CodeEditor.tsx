import CodeEditorComponent from "@uiw/react-textarea-code-editor";
import { useMemo } from "react";

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

const CodeEditor = ({
  onChange,
  height = 128,
  value,
  language,
  ...rest
}: CodeEditorProps) => {
  const codeEditor = useMemo(
    () => (
      <CodeEditorComponent
        padding={15}
        minHeight={height}
        style={{
          font: "var(--pf-global--FontFamily--monospace)",
        }}
        onChange={(event) => onChange?.(event.target.value)}
        value={value}
        language={language}
        {...rest}
      />
    ),
    [value, language],
  );

  return (
    <div style={{ height: `${height}px`, overflow: "auto" }}>{codeEditor}</div>
  );
};

export default CodeEditor;
