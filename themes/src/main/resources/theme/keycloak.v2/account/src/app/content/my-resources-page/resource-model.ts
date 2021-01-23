export interface Resource {
  _id: string;
  name: string;
  client: Client;
  scopes: Scope[];
  uris: string[];
  shareRequests: Permission[];
}

export interface Client {
  baseUrl: string;
  clientId: string;
  name?: string;
}

export class Scope {
  public constructor(public name: string, public displayName?: string) {}

  public toString(): string {
      if (this.hasOwnProperty('displayName') && (this.displayName)) {
          return this.displayName;
      } else {
          return this.name;
      }
  }
}

export interface PaginatedResources {
  nextUrl: string;
  prevUrl: string;
  data: Resource[];
}

export interface Permission {
  email?: string;
  firstName?: string;
  lastName?: string;
  scopes: Scope[] | string[];  // this should be Scope[] - fix API
  username: string;
}
