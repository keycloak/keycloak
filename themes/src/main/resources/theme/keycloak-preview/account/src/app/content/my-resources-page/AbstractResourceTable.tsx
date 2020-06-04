import * as React from 'react';
import { Permission, PaginatedResources, Client } from './MyResourcesPage';
import { Msg } from '../../widgets/Msg';

export interface ResourcesTableProps {
  resources: PaginatedResources;
}

export interface ResourcesTableState {
  permissions: Map<number, Permission[]>;
}

export abstract class AbstractResourcesTable<S extends ResourcesTableState> extends React.Component<ResourcesTableProps, S> {

  protected hasPermissions(row: number): boolean {
    return (this.state.permissions.has(row)) && (this.state.permissions.get(row)!.length > 0);
  }

  private firstUser(row: number): string {
    if (!this.hasPermissions(row)) return 'ERROR!!!!'; // should never happen

    return this.state.permissions.get(row)![0].username;
  }

  protected numOthers(row: number): number {
    if (!this.hasPermissions(row)) return -1; // should never happen

    return this.state.permissions.get(row)!.length - 1;
  }

  public sharedWithUsersMessage(row: number): React.ReactNode {
    if (!this.hasPermissions(row)) return (<React.Fragment><Msg msgKey='resourceNotShared' /></React.Fragment>);

    return (
      <React.Fragment>
        <Msg msgKey='resourceSharedWith'>
          <strong>{this.firstUser(row)}</strong>
        </Msg>
        {this.numOthers(row) > 0 && <Msg msgKey='and'>
          <strong>{this.numOthers(row)}</strong>
        </Msg>}.
      </React.Fragment>
    );
  }

  protected getClientName(client: Client): string {
    if (client.hasOwnProperty('name') && client.name !== null && client.name !== '') {
      return Msg.localize(client.name!);
    } else {
      return client.clientId;
    }
  }
}
