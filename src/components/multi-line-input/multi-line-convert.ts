export type MultiLine = {
  value: string;
};

export function convertToMultiline(fields: string[]): MultiLine[] {
  return (fields.length > 0 ? fields : [""]).map((field) => {
    return { value: field };
  });
}

export function toValue(formValue: MultiLine[]): string[] {
  return formValue.map((field) => field.value);
}
