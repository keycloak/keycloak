import { UserSelect } from "../../../components/users/UserSelect";

export const User = () => (
  <UserSelect
    name="users"
    label="users"
    helpText="clients-help:policyUsers"
    defaultValue={[]}
    isRequired
  />
);
