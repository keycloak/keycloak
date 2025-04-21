import React from 'react';
import {
  MenuToggle,
  Menu,
  MenuContent,
  MenuList,
  MenuItem,
  DrilldownMenu,
  Divider,
  Popper
} from '@patternfly/react-core';
import StorageDomainIcon from '@patternfly/react-icons/dist/esm/icons/storage-domain-icon';
import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import LayerGroupIcon from '@patternfly/react-icons/dist/esm/icons/layer-group-icon';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';

interface MenuHeightsType {
  [id: string]: number;
}

export const ComposableDrilldownMenu: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [activeMenu, setActiveMenu] = React.useState<string>('rootMenu');
  const [menuDrilledIn, setMenuDrilledIn] = React.useState<string[]>([]);
  const [drilldownPath, setDrilldownPath] = React.useState<string[]>([]);
  const [menuHeights, setMenuHeights] = React.useState<MenuHeightsType>({});
  const toggleRef = React.useRef<HTMLButtonElement>();
  const menuRef = React.useRef<HTMLDivElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (isOpen && menuRef.current.contains(event.target as Node)) {
      if (event.key === 'Escape' || event.key === 'Tab') {
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }
    }
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (isOpen && !menuRef.current.contains(event.target as Node)) {
      setIsOpen(false);
    }
  };

  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, menuRef]);

  const onToggleClick = (ev: React.MouseEvent) => {
    ev.stopPropagation(); // Stop handleClickOutside from handling
    setTimeout(() => {
      if (menuRef.current) {
        const firstElement = menuRef.current.querySelector('li > button:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
    setMenuDrilledIn([]);
    setDrilldownPath([]);
    setActiveMenu('rootMenu');
  };

  const drillIn = (fromMenuId: string, toMenuId: string, pathId: string) => {
    setMenuDrilledIn([...menuDrilledIn, fromMenuId]);
    setDrilldownPath([...drilldownPath, pathId]);
    setActiveMenu(toMenuId);
  };

  const drillOut = (toMenuId: string) => {
    setMenuDrilledIn(menuDrilledIn.slice(0, menuDrilledIn.length - 1));
    setDrilldownPath(drilldownPath.slice(0, drilldownPath.length - 1));
    setActiveMenu(toMenuId);
  };

  const setHeight = (menuId: string, height: number) => {
    if (!menuHeights[menuId]) {
      setMenuHeights({
        ...menuHeights,
        [menuId]: height
      });
    }
  };

  const toggle = (
    <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen}>
      {isOpen ? 'Expanded' : 'Collapsed'}
    </MenuToggle>
  );
  const menu = (
    <Menu
      id="rootMenu"
      containsDrilldown
      drilldownItemPath={drilldownPath}
      drilledInMenus={menuDrilledIn}
      activeMenu={activeMenu}
      onDrillIn={drillIn}
      onDrillOut={drillOut}
      onGetMenuHeight={setHeight}
      ref={menuRef}
      style={
        {
          '--pf-c-menu--Width': '300px'
        } as React.CSSProperties
      }
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
                        Application Grouping
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="group_a">Group A</MenuItem>
                      <MenuItem itemId="group_b">Group B</MenuItem>
                      <MenuItem itemId="group_c">Group C</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Application Grouping
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
                        Application Grouping
                      </MenuItem>
                      <Divider component="li" />
                      <MenuItem itemId="group_a">Group A</MenuItem>
                      <MenuItem itemId="group_b">Group B</MenuItem>
                      <MenuItem itemId="group_c">Group C</MenuItem>
                    </DrilldownMenu>
                  }
                >
                  Application Grouping
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
                  From Git
                </MenuItem>
                <MenuItem icon={<LayerGroupIcon aria-hidden />} itemId="container">
                  Container Image
                </MenuItem>
                <MenuItem icon={<CubeIcon aria-hidden />} itemId="docker">
                  Docker File
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
  return (
    <div ref={containerRef}>
      <Popper
        trigger={toggle}
        popper={menu}
        appendTo={containerRef.current}
        isVisible={isOpen}
        popperMatchesTriggerWidth={false}
      />
    </div>
  );
};
