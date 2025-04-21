import React from 'react';
import {
  MenuToggle,
  Panel,
  PanelMain,
  PanelMainBody,
  Title,
  Popper,
  TreeView,
  TreeViewDataItem
} from '@patternfly/react-core';

export const ComposableTreeViewMenu: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [checkedItems, setCheckedItems] = React.useState<TreeViewDataItem[]>([]);
  const toggleRef = React.useRef<HTMLButtonElement>();
  const containerRef = React.useRef<HTMLDivElement>();
  const menuRef = React.useRef<HTMLDivElement>();

  const statusOptions: TreeViewDataItem[] = [
    {
      name: 'Ready',
      id: 'ready',
      checkProps: { checked: false },
      customBadgeContent: 1,
      children: [
        {
          name: 'Updated',
          id: 'updated',
          checkProps: { checked: false },
          customBadgeContent: 0
        },
        {
          name: 'Waiting to update',
          id: 'waiting',
          checkProps: { checked: false },
          customBadgeContent: 0
        },
        {
          name: 'Conditions degraded',
          id: 'degraded',
          checkProps: { checked: false },
          customBadgeContent: 1
        },
        {
          name: 'Approval required',
          id: 'approval',
          checkProps: { checked: false },
          customBadgeContent: 0
        }
      ]
    },
    {
      name: 'Not ready',
      id: 'nr',
      checkProps: { checked: false },
      customBadgeContent: 1,
      children: [
        {
          name: 'Conditions degraded',
          id: 'nr-degraded',
          checkProps: { checked: false },
          customBadgeContent: 1
        }
      ]
    },
    {
      name: 'Updating',
      id: 'updating',
      checkProps: { checked: false },
      customBadgeContent: 0
    }
  ];

  const roleOptions = [
    {
      name: 'Server',
      id: 'server',
      checkProps: { checked: false },
      customBadgeContent: 2
    },
    {
      name: 'Worker',
      id: 'worker',
      checkProps: { checked: false },
      customBadgeContent: 0
    }
  ];
  // Helper functions for tree
  const isChecked = (dataItem: TreeViewDataItem) => checkedItems.some(item => item.id === dataItem.id);
  const areAllDescendantsChecked = (dataItem: TreeViewDataItem) =>
    dataItem.children ? dataItem.children.every(child => areAllDescendantsChecked(child)) : isChecked(dataItem);
  const areSomeDescendantsChecked = (dataItem: TreeViewDataItem) =>
    dataItem.children ? dataItem.children.some(child => areSomeDescendantsChecked(child)) : isChecked(dataItem);
  const flattenTree = (tree: TreeViewDataItem[]) => {
    let result = [];
    tree.forEach(item => {
      result.push(item);
      if (item.children) {
        result = result.concat(flattenTree(item.children));
      }
    });
    return result;
  };

  const mapTree = (item: TreeViewDataItem) => {
    const hasCheck = areAllDescendantsChecked(item);
    // Reset checked properties to be updated
    item.checkProps.checked = false;

    if (hasCheck) {
      item.checkProps.checked = true;
    } else {
      const hasPartialCheck = areSomeDescendantsChecked(item);
      if (hasPartialCheck) {
        item.checkProps.checked = null;
      }
    }

    if (item.children) {
      return {
        ...item,
        children: item.children.map(mapTree)
      };
    }
    return item;
  };

  const filterItems = (item: TreeViewDataItem, checkedItem: TreeViewDataItem) => {
    if (item.id === checkedItem.id) {
      return true;
    }

    if (item.children) {
      return (
        (item.children = item.children
          .map(opt => Object.assign({}, opt))
          .filter(child => filterItems(child, checkedItem))).length > 0
      );
    }
  };

  const onCheck = (evt: React.ChangeEvent, treeViewItem: TreeViewDataItem, treeType: string) => {
    const checked = (evt.target as HTMLInputElement).checked;

    let options = [];
    switch (treeType) {
      case 'status':
        options = statusOptions;
        break;
      case 'role':
        options = roleOptions;
        break;
      default:
        break;
    }

    const checkedItemTree = options.map(opt => Object.assign({}, opt)).filter(item => filterItems(item, treeViewItem));
    const flatCheckedItems = flattenTree(checkedItemTree);
    setCheckedItems(prevCheckedItems =>
      checked
        ? prevCheckedItems.concat(flatCheckedItems.filter(item => !prevCheckedItems.some(i => i.id === item.id)))
        : prevCheckedItems.filter(item => !flatCheckedItems.some(i => i.id === item.id))
    );
  };

  // Controls keys that should open/close the menu
  const handleMenuKeys = (event: KeyboardEvent) => {
    if (!isOpen) {
      return;
    }
    if (menuRef.current.contains(event.target as Node) || toggleRef.current.contains(event.target as Node)) {
      // The escape key when pressed while inside the menu should close the menu and refocus the toggle
      if (event.key === 'Escape') {
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }

      // The tab key when pressed while inside the menu and on the contained last tree view should close the menu and refocus the toggle
      // Shift tab should keep the default behavior to return to a previous tree view
      if (event.key === 'Tab' && !event.shiftKey) {
        const treeList = menuRef.current.querySelectorAll('.pf-c-tree-view');
        if (treeList[treeList.length - 1].contains(event.target as Node)) {
          event.preventDefault();
          setIsOpen(!isOpen);
          toggleRef.current.focus();
        }
      }
    }
  };

  // Controls that a click outside the menu while the menu is open should close the menu
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
        const firstElement = menuRef.current.querySelector('li button:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const toggle = (
    <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen}>
      {isOpen ? 'Expanded' : 'Collapsed'}
    </MenuToggle>
  );
  const statusMapped = statusOptions.map(mapTree);
  const roleMapped = roleOptions.map(mapTree);
  const menu = (
    <Panel
      ref={menuRef}
      variant="raised"
      style={{
        width: '300px'
      }}
    >
      <PanelMain>
        <section>
          <PanelMainBody style={{ paddingBottom: 0 }}>
            <Title headingLevel="h1" size={'md'}>
              Status
            </Title>
          </PanelMainBody>
          <PanelMainBody style={{ padding: 0 }}>
            <TreeView
              data={statusMapped}
              hasBadges
              hasChecks
              onCheck={(event, item) => onCheck(event, item, 'status')}
            />
          </PanelMainBody>
        </section>
        <section>
          <PanelMainBody style={{ paddingBottom: 0, paddingTop: 0 }}>
            <Title headingLevel="h1" size={'md'}>
              Roles
            </Title>
          </PanelMainBody>
          <PanelMainBody style={{ padding: 0 }}>
            <TreeView data={roleMapped} hasBadges hasChecks onCheck={(event, item) => onCheck(event, item, 'role')} />
          </PanelMainBody>
        </section>
      </PanelMain>
    </Panel>
  );
  return (
    <div ref={containerRef}>
      <Popper
        trigger={toggle}
        popper={menu}
        isVisible={isOpen}
        appendTo={containerRef.current}
        popperMatchesTriggerWidth={false}
      />
    </div>
  );
};
