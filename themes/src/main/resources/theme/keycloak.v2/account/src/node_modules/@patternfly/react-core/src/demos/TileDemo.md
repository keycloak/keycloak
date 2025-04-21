---
id: Tile
section: components
---

## Demos

### Tiles with single selection

```ts
import React from 'react';
import { Tile } from '@patternfly/react-core';

const TileSingleSelect: React.FunctionComponent = () => {
  const [selectedId, setSelectedId] = React.useState<string>('');

  const onSelect = (event: React.MouseEvent) => {
    setSelectedId(event.currentTarget.id);
  };

  const onKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === ' ' || event.key === 'Enter') {
      event.preventDefault();
      setSelectedId(event.currentTarget.id);
    }
  };

  return (
    <div role="listbox" aria-label="Single selection tiles">
      <Tile title="Tile 1" id="tile-1" onClick={onSelect} onKeyDown={onKeyDown} isSelected={selectedId === 'tile-1'} />
      <Tile title="Tile 2" id="tile-2" onClick={onSelect} onKeyDown={onKeyDown} isSelected={selectedId === 'tile-2'} />
      <Tile title="Tile 3" id="tile-3" isDisabled isSelected={selectedId === 'tile-3'} />
      <Tile title="Tile 4" id="tile-4" onClick={onSelect} onKeyDown={onKeyDown} isSelected={selectedId === 'tile-4'} />
    </div>
  );
};
```

### Tiles with multiple selection

```ts
import React from 'react';
import { Tile } from '@patternfly/react-core';

const TileMultiSelect: React.FunctionComponent = () => {
  const [selectedIds, setSelectedIds] = React.useState<string[]>([]);

  const onSelect = (event: React.MouseEvent | React.KeyboardEvent) => {
    const targetId = event.currentTarget.id;
    setSelectedIds(prevSelectedIds => {
      const newSelectedIds = prevSelectedIds.includes(targetId)
        ? prevSelectedIds.filter(id => id !== targetId)
        : [...prevSelectedIds, targetId];
      console.log('tile selections: ', newSelectedIds);
      return newSelectedIds;
    });
  };

  const onKeyDown = (event: React.KeyboardEvent) => {
    console.log(event.key);
    if (event.key === ' ' || event.key === 'Enter') {
      event.preventDefault();
      onSelect(event);
    }
  };

  return (
    <div role="listbox" aria-multiselectable={true} aria-label="Multiselectable tiles">
      <Tile
        title="Tile 1"
        id="tile-1"
        onClick={onSelect}
        onKeyDown={onKeyDown}
        isSelected={selectedIds.includes('tile-1')}
      />
      <Tile
        title="Tile 2"
        id="tile-2"
        onClick={onSelect}
        onKeyDown={onKeyDown}
        isSelected={selectedIds.includes('tile-2')}
      />
      <Tile title="Tile 3" id="tile-3" isDisabled />
      <Tile
        title="Tile 4"
        id="tile-4"
        onClick={onSelect}
        onKeyDown={onKeyDown}
        isSelected={selectedIds.includes('tile-4')}
      />
    </div>
  );
};
```
