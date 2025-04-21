import React from 'react';
import { Menu, MenuContent, MenuList, MenuItem, Divider, DrilldownMenu } from '@patternfly/react-core';
import StorageDomainIcon from '@patternfly/react-icons/dist/esm/icons/storage-domain-icon';
import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import LayerGroupIcon from '@patternfly/react-icons/dist/esm/icons/layer-group-icon';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';

export const MenuDrilldown: React.FunctionComponent = () => {
  const [menuDrilledIn, setMenuDrilledIn] = React.useState<string[]>([]);
  const [drilldownPath, setDrilldownPath] = React.useState<string[]>([]);
  const [menuHeights, setMenuHeights] = React.useState<any>({});
  const [activeMenu, setActiveMenu] = React.useState<string>('rootMenu');

  const drillIn = (fromMenuId: string, toMenuId: string, pathId: string) => {
    setMenuDrilledIn([...menuDrilledIn, fromMenuId]);
    setDrilldownPath([...drilldownPath, pathId]);
    setActiveMenu(toMenuId);
  };

  const drillOut = (toMenuId: string) => {
    const menuDrilledInSansLast = menuDrilledIn.slice(0, menuDrilledIn.length - 1);
    const pathSansLast = drilldownPath.slice(0, drilldownPath.length - 1);
    setMenuDrilledIn(menuDrilledInSansLast);
    setDrilldownPath(pathSansLast);
    setActiveMenu(toMenuId);
  };

  const setHeight = (menuId: string, height: number) => {
    if (menuHeights[menuId] === undefined) {
      setMenuHeights({ ...menuHeights, [menuId]: height });
    }
  };

  return (
    <Menu
      id="rootMenu"
      containsDrilldown
      drilldownItemPath={drilldownPath}
      drilledInMenus={menuDrilledIn}
      activeMenu={activeMenu}
      onDrillIn={drillIn}
      onDrillOut={drillOut}
      onGetMenuHeight={setHeight}
    >
      <MenuContent menuHeight={`${menuHeights[activeMenu]}px`}>
        <MenuList>
          <MenuItem
            itemId="group:start_rollout"
            direction="down"
            drilldownMenu={
              <DrilldownMenu id="drilldownMenuStart">
                <MenuItem itemId="group:start_rollout_breadcrumb" direction="up">
                  Start rollout
                </MenuItem>
                <Divider component="li" />
                <MenuItem
                  itemId="group:app_grouping"
                  description="Groups A-C"
                  direction="down"
                  drilldownMenu={
                    <DrilldownMenu id="drilldownMenuStartGrouping">
                      <MenuItem itemId="group:app_grouping_breadcrumb" direction="up">
                        Application grouping
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="group_a">Group A</MenuItem>
                      <MenuItem itemId="group_b">Group B</MenuItem>
                      <MenuItem itemId="group_c">Group C</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Application grouping
                </MenuItem>
                <MenuItem itemId="count">Count</MenuItem>
                <MenuItem
                  itemId="group:labels"
                  direction="down"
                  drilldownMenu={
                    <DrilldownMenu id="drilldownMenuStartLabels">
                      <MenuItem itemId="group:labels_breadcrumb" direction="up">
                        Labels
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="label_1">Label 1</MenuItem>
                      <MenuItem itemId="label_2">Label 2</MenuItem>
                      <MenuItem itemId="label_3">Label 3</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Labels
                </MenuItem>
                <MenuItem itemId="annotations">Annotations</MenuItem>
              </DrilldownMenu>
            }
          >
            Start rollout
          </MenuItem>
          <MenuItem
            itemId="group:pause_rollout"
            direction="down"
            drilldownMenu={
              <DrilldownMenu id="drilldownMenuPause">
                <MenuItem itemId="group:pause_rollout_breadcrumb" direction="up">
                  Pause rollouts
                </MenuItem>
                <Divider component="li" />
                <MenuItem
                  itemId="group:app_grouping"
                  description="Groups A-C"
                  direction="down"
                  drilldownMenu={
                    <DrilldownMenu id="drilldownMenuGrouping">
                      <MenuItem itemId="group:app_grouping_breadcrumb" direction="up">
                        Application grouping
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="group_a">Group A</MenuItem>
                      <MenuItem itemId="group_b">Group B</MenuItem>
                      <MenuItem itemId="group_c">Group C</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Application grouping
                </MenuItem>
                <MenuItem itemId="count">Count</MenuItem>
                <MenuItem
                  itemId="group:labels"
                  direction="down"
                  drilldownMenu={
                    <DrilldownMenu id="drilldownMenuLabels">
                      <MenuItem itemId="group:labels_breadcrumb" direction="up">
                        Labels
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="label_1">Label 1</MenuItem>
                      <MenuItem itemId="label_2">Label 2</MenuItem>
                      <MenuItem itemId="label_3">Label 3</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Labels
                </MenuItem>
                <MenuItem itemId="annotations">Annotations</MenuItem>
              </DrilldownMenu>
            }
          >
            Pause rollouts
          </MenuItem>
          <MenuItem
            itemId="group:storage"
            icon={<StorageDomainIcon aria-hidden />}
            direction="down"
            drilldownMenu={
              <DrilldownMenu id="drilldownMenuStorage">
                <MenuItem itemId="group:storage_breadcrumb" icon={<StorageDomainIcon aria-hidden />} direction="up">
                  Add storage
                </MenuItem>
                <Divider component="li" />
                <MenuItem icon={<CodeBranchIcon aria-hidden />} itemId="git">
                  From git
                </MenuItem>
                <MenuItem icon={<LayerGroupIcon aria-hidden />} itemId="container">
                  Container image
                </MenuItem>
                <MenuItem icon={<CubeIcon aria-hidden />} itemId="docker">
                  Docker file
                </MenuItem>
              </DrilldownMenu>
            }
          >
            Add storage
          </MenuItem>
          <MenuItem itemId="edit">Edit</MenuItem>
          <MenuItem itemId="delete_deployment">Delete deployment config</MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );
};
