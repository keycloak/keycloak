import { UserSelect } from "../../../components/users/hook-form-v7/UserSelect";

export const User = () => (
  <UserSelect
    name="users"
    label="users"
    helpText="clients-help:policyUsers"
    defaultValue={[]}
    isRequired
  />
);
