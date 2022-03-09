export function stringToMultiline(value?: string): string[] {
  return (value || "").split("##");
}

export function toStringValue(formValue: string[]): string {
  return formValue.join("##");
}
