import * as React from 'react';

import {
  Checkbox,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
} from '@patternfly/react-core';

import { ContentPage } from '../ContentPage';
import { HttpResponse } from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Msg } from '../../widgets/Msg';

export interface GroupsPageProps {
}

export interface GroupsPageState {
  groups: Group[];
  directGroups: Group[];
  isDirectMembership: boolean;
}

interface Group {
  id?: string;
  name: string;
  path: string;
}

export class GroupsPage extends React.Component<GroupsPageProps, GroupsPageState> {
  static contextType = AccountServiceContext;
  context: React.ContextType<typeof AccountServiceContext>;

  public constructor(props: GroupsPageProps, context: React.ContextType<typeof AccountServiceContext>) {
    super(props);
    this.context = context;
    this.state = {
      groups: [],
      directGroups: [],
      isDirectMembership: false
    };

    this.fetchGroups();
  }

  private fetchGroups(): void {
    this.context!.doGet<Group[]>("/groups")
      .then((response: HttpResponse<Group[]>) => {
        const directGroups = response.data || [];
        const groups = [...directGroups];
        const groupsPaths = directGroups.map(s => s.path);
        directGroups.forEach((el) => this.getParents(el, groups, groupsPaths))
        this.setState({
          groups: groups,
          directGroups: directGroups
        });
      });
  }

  private getParents(el: Group, groups: Group[], groupsPaths: string[]): void {
    const parentPath = el.path.slice(0, el.path.lastIndexOf('/'));
    if (parentPath && (groupsPaths.indexOf(parentPath) === -1)) {

      el = {
        name: parentPath.slice(parentPath.lastIndexOf('/')+1),
        path: parentPath
      };
      groups.push(el);
      groupsPaths.push(parentPath);

      this.getParents(el, groups, groupsPaths);
    }
  }

  private changeDirectMembership = (checked: boolean,event: React.FormEvent<HTMLInputElement> )=> {
    this.setState({
      isDirectMembership: checked
    });
  }

  private emptyGroup(): React.ReactNode {

    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey='noGroupsText' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }

  private renderGroupList(group: Group, appIndex: number): React.ReactNode {

    return (
      <DataListItem id={`${appIndex}-group`} key={'group-' + appIndex} aria-labelledby="groups-list" >
        <DataListItemRow>
          <DataListItemCells
            dataListCells={[
              <DataListCell id={`${appIndex}-group-name`} width={2} key={'name-' + appIndex}>
                {group.name}
              </DataListCell>,
              <DataListCell id={`${appIndex}-group-path`} width={2} key={'path-' + appIndex}>
                {group.path}
              </DataListCell>,
              <DataListCell id={`${appIndex}-group-directMembership`} width={2} key={'directMembership-' + appIndex}>
                <Checkbox id={`${appIndex}-checkbox-directMembership`} isChecked={group.id != null} isDisabled={true} />
              </DataListCell>
            ]}
          />
        </DataListItemRow>

      </DataListItem>
    )
  }

  public render(): React.ReactNode {
    return (
      <ContentPage title={Msg.localize('groupLabel')}>
        <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact>
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow >
              <DataListItemCells
                dataListCells={[
                  <DataListCell key='directMembership-header' >
                    <Checkbox
                      label={Msg.localize('directMembership')}
                      id="directMembership-checkbox"
                      isChecked={this.state.isDirectMembership}
                      onChange={this.changeDirectMembership}
                    />

                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow >
              <DataListItemCells
                dataListCells={[
                  <DataListCell key='group-name-header' width={2}>
                    <strong><Msg msgKey='Name' /></strong>
                  </DataListCell>,
                  <DataListCell key='group-path-header' width={2}>
                    <strong><Msg msgKey='path' /></strong>
                  </DataListCell>,
                  <DataListCell key='group-direct-membership-header' width={2}>
                    <strong><Msg msgKey='directMembership' /></strong>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {this.state.groups.length === 0
            ? this.emptyGroup()
            : (this.state.isDirectMembership ? this.state.directGroups.map((group: Group, appIndex: number) =>
              this.renderGroupList(group, appIndex)
            ) : this.state.groups.map((group: Group, appIndex: number) =>
              this.renderGroupList(group, appIndex)))}
        </DataList>
      </ContentPage>
    );
  }
};
