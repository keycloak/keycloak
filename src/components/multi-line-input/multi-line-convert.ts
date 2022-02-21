export type MultiLine = {
  value: string;
};

export function convertToMultiline(fields: string[]): MultiLine[] {
  return (fields.length > 0 ? fields : [""]).map((field) => ({ value: field }));
}

export function stringToMultiline(value?: string): MultiLine[] {
  return (value || "").split("##").map((v) => ({ value: v }));
}

export function toStringValue(formValue: MultiLine[]): string {
  return formValue.map((field) => field.value).join("##");
}

export function toValue(formValue: MultiLine[]): string[] {
  return formValue.map((field) => field.value);
}
