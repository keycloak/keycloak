export default interface PermissionRepresentation {
  claims?: { [index: string]: string };
  rsid?: string;
  rsname?: string;
  scopes?: string[];
}
