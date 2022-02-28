import * as React from "../../../../common/keycloak/web_modules/react.js";
import { Msg } from "../../widgets/Msg.js";
export class AbstractResourcesTable extends React.Component {
  hasPermissions(row) {
    return this.state.permissions.has(row) && this.state.permissions.get(row).length > 0;
  }

  firstUser(row) {
    if (!this.hasPermissions(row)) return 'ERROR!!!!'; // should never happen

    return this.state.permissions.get(row)[0].username;
  }

  numOthers(row) {
    if (!this.hasPermissions(row)) return -1; // should never happen

    return this.state.permissions.get(row).length - 1;
  }

  sharedWithUsersMessage(row) {
    if (!this.hasPermissions(row)) return React.createElement(React.Fragment, null, React.createElement(Msg, {
      msgKey: "resourceNotShared"
    }));
    return React.createElement(React.Fragment, null, React.createElement(Msg, {
      msgKey: "resourceSharedWith"
    }, React.createElement("strong", null, this.firstUser(row))), this.numOthers(row) > 0 && React.createElement(Msg, {
      msgKey: "and"
    }, React.createElement("strong", null, this.numOthers(row))), ".");
  }

  getClientName(client) {
    if (client.hasOwnProperty('name') && client.name !== null && client.name !== '') {
      return Msg.localize(client.name);
    } else {
      return client.clientId;
    }
  }

}
//# sourceMappingURL=AbstractResourceTable.js.map