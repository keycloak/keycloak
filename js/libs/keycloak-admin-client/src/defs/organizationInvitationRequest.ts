export interface InviteUserRequest {
  email: string;
  firstName?: string;
  lastName?: string;
  attributes?: Record<string, string[]>;
}

export interface InviteExistingUserRequest {
  id: string;
  attributes?: Record<string, string[]>;
}
