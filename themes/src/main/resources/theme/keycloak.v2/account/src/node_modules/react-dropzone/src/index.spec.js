/* eslint jsx-a11y/click-events-have-key-events: 0 */
/* eslint jsx-a11y/no-static-element-interactions: 0 */

import React from 'react'
import { mount, render } from 'enzyme'
import { fromEvent } from 'file-selector'
import * as utils from './utils'

const flushPromises = wrapper =>
  new Promise(resolve =>
    global.setImmediate(() => {
      wrapper.update()
      resolve(wrapper)
    })
  )
const Dropzone = require('./index')
const DummyChildComponent = () => null

const createFile = (name, size, type) => {
  const file = new File([], name, { type })
  Object.defineProperty(file, 'size', {
    get() {
      return size
    }
  })
  return file
}

const createDtWithFiles = (files = []) => {
  return {
    dataTransfer: {
      files,
      items: files.map(file => ({
        kind: 'file',
        type: file.type,
        getAsFile: () => file
      })),
      types: ['Files']
    }
  }
}

const createDtWithItems = (items, types) => {
  return {
    dataTransfer: { items, types }
  }
}

const createDtWithTextTypes = () => {
  return createDtWithItems([], ['text/html', 'text/plain'])
}

const createDtWithTextItems = () => {
  return createDtWithItems([{ kind: 'string', type: 'text/plain' }], ['text/html', 'text/plain'])
}

let files
let images

describe('Dropzone', () => {
  beforeEach(() => {
    files = [createFile('file1.pdf', 1111, 'application/pdf')]
    images = [createFile('cats.gif', 1234, 'image/gif'), createFile('dogs.gif', 2345, 'image/jpeg')]
  })

  describe('basics', () => {
    it('should render children', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(dropzone.html()).toMatchSnapshot()
    })

    it('sets refs properly', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(dropzone.instance().node).not.toBeUndefined()
      expect(dropzone.instance().node.tagName).toEqual('DIV')
      expect(dropzone.instance().input).not.toBeUndefined()
      expect(dropzone.instance().input.tagName).toEqual('INPUT')
    })

    it('applies the accept prop to the child input', () => {
      const component = render(
        <Dropzone accept="image/jpeg">
          {({ getRootProps, getInputProps }) => (
            <div className="my-dropzone" {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      expect(component.attr()).not.toContain('accept')
      expect(Object.keys(component.find('input').attr())).toContain('accept')
      expect(component.find('input').attr('accept')).toEqual('image/jpeg')
    })

    it('applies the name prop to the child input', () => {
      const component = render(
        <Dropzone name="test-file-input">
          {({ getRootProps, getInputProps }) => (
            <div className="my-dropzone" {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(component.attr()).not.toContain('name')
      expect(Object.keys(component.find('input').attr())).toContain('name')
      expect(component.find('input').attr('name')).toEqual('test-file-input')
    })

    it('runs custom root handlers', async () => {
      const evt = createDtWithFiles(files)
      const rootProps = {
        onClick: jest.fn(),
        onKeyDown: jest.fn(),
        onFocus: jest.fn(),
        onBlur: jest.fn(),
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn(),
        onDrop: jest.fn()
      }
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps(rootProps)}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('click')
      await flushPromises(dropzone)
      expect(rootProps.onClick).toHaveBeenCalled()

      dropzone.simulate('focus')
      expect(rootProps.onFocus).toHaveBeenCalled()

      dropzone.simulate('blur')
      expect(rootProps.onBlur).toHaveBeenCalled()

      dropzone.simulate('keydown')
      expect(rootProps.onKeyDown).toHaveBeenCalled()

      await dropzone.simulate('dragStart', evt)
      await flushPromises(dropzone)
      expect(rootProps.onDragStart).toHaveBeenCalled()

      await dropzone.simulate('dragEnter', evt)
      await flushPromises(dropzone)
      expect(rootProps.onDragEnter).toHaveBeenCalled()

      await dropzone.simulate('dragOver', evt)
      await flushPromises(dropzone)
      expect(rootProps.onDragOver).toHaveBeenCalled()

      await dropzone.simulate('dragLeave', evt)
      await flushPromises(dropzone)
      expect(rootProps.onDragLeave).toHaveBeenCalled()

      await dropzone.simulate('drop', evt)
      await flushPromises(dropzone)
      expect(rootProps.onDrop).toHaveBeenCalled()
    })
  })

  describe('document drop protection', () => {
    const event = { preventDefault: jest.fn() }
    const onAddEventListener = jest.spyOn(document, 'addEventListener')
    const onRemoveEventListener = jest.spyOn(document, 'removeEventListener')

    // Collect the list of addEventListener/removeEventListener spy calls into an object keyed by event name.
    function collectEventListenerCalls(calls) {
      return calls.reduce(
        (acc, [eventName, ...rest]) => ({
          ...acc,
          [eventName]: rest
        }),
        {}
      )
    }

    it('installs hooks to prevent stray drops from taking over the browser window', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Content</p>
            </div>
          )}
        </Dropzone>
      )
      expect(dropzone.html()).toMatchSnapshot()
      expect(onAddEventListener).toHaveBeenCalledTimes(2)
      const addEventCalls = collectEventListenerCalls(onAddEventListener.mock.calls)
      Object.keys(addEventCalls).forEach(eventName => {
        expect(addEventCalls[eventName][0]).toBeDefined()
        expect(addEventCalls[eventName][1]).toBe(false)
      })
    })

    it('terminates drags and drops on elements outside our dropzone', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Content</p>
            </div>
          )}
        </Dropzone>
      )

      utils.onDocumentDragOver(event)
      expect(event.preventDefault).toHaveBeenCalledTimes(1)
      event.preventDefault.mockClear()

      dropzone.instance().onDocumentDrop(event)
      expect(event.preventDefault).toHaveBeenCalledTimes(1)
    })

    it('permits drags and drops on elements inside our dropzone', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Content</p>
            </div>
          )}
        </Dropzone>
      )

      const instanceEvent = {
        preventDefault: jest.fn(),
        target: dropzone.getDOMNode()
      }
      dropzone.instance().onDocumentDrop(instanceEvent)
      expect(instanceEvent.preventDefault).not.toHaveBeenCalled()
    })

    it('removes document hooks when unmounted', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Content</p>
            </div>
          )}
        </Dropzone>
      )
      dropzone.unmount()
      expect(onRemoveEventListener).toHaveBeenCalledTimes(2)
      const addEventCalls = collectEventListenerCalls(onAddEventListener.mock.calls)
      const removeEventCalls = collectEventListenerCalls(onRemoveEventListener.mock.calls)
      Object.keys(addEventCalls).forEach(eventName => {
        expect(removeEventCalls[eventName][0]).toEqual(addEventCalls[eventName][0])
      })
    })

    it('does not prevent stray drops when preventDropOnDocument is false', () => {
      const dropzone = mount(
        <Dropzone preventDropOnDocument={false}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(dropzone.html()).toMatchSnapshot()
      expect(onAddEventListener).not.toHaveBeenCalled()

      dropzone.unmount()
      expect(onRemoveEventListener).not.toHaveBeenCalled()
    })
  })

  describe('onClick', () => {
    it('should call `open` method', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('click')
      expect(open).toHaveBeenCalled()
    })

    it('should call `onClick` callback if provided', () => {
      const onClick = jest.fn()
      const dropzone = mount(
        <Dropzone onClick={onClick}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('click')
      expect(open).toHaveBeenCalled()
      expect(onClick).toHaveBeenCalled()
    })

    it('should not call `open` if event was prevented in `onClick`', () => {
      const onClick = jest.fn(event => event.preventDefault())
      const dropzone = mount(
        <Dropzone onClick={onClick}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('click')
      expect(open).toHaveBeenCalledTimes(0)
      expect(onClick).toHaveBeenCalled()
    })

    it('should reset the value of input', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(
        dropzone
          .render()
          .find('input')
          .attr('value')
      ).toBeUndefined()
      expect(
        dropzone
          .render()
          .find('input')
          .attr('value', 10)
      ).not.toBeUndefined()
      dropzone.simulate('click')
      expect(
        dropzone
          .render()
          .find('input')
          .attr('value')
      ).toBeUndefined()
    })

    it('should trigger click even on the input', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const onFileInputClick = jest.spyOn(dropzone.instance().input, 'click')
      dropzone.simulate('click')
      dropzone.simulate('click')
      expect(onFileInputClick).toHaveBeenCalledTimes(2)
    })

    it('should not invoke onClick on the wrapper', () => {
      const onClickOuter = jest.fn()
      const onClickInner = jest.fn()
      const component = mount(
        <div onClick={onClickOuter}>
          <Dropzone onClick={onClickInner}>
            {({ getRootProps, getInputProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
              </div>
            )}
          </Dropzone>
        </div>
      )

      component.simulate('click')
      expect(onClickOuter).toHaveBeenCalled()
      expect(onClickInner).not.toHaveBeenCalled()

      onClickOuter.mockClear()
      onClickInner.mockClear()

      component.find(Dropzone).simulate('click')
      expect(onClickOuter).not.toHaveBeenCalled()
      expect(onClickInner).toHaveBeenCalled()
    })

    it('should invoke inputProps onClick if provided', () => {
      const onClick = jest.fn()
      const component = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps({ onClick })} />
            </div>
          )}
        </Dropzone>
      )

      component.find('input').simulate('click')
      expect(onClick).toHaveBeenCalled()
    })

    it('should schedule open() on next tick when Edge', () => {
      const isIeOrEdgeSpy = jest.spyOn(utils, 'isIeOrEdge').mockReturnValueOnce(true)
      const setTimeoutSpy = jest.spyOn(window, 'setTimeout').mockImplementationOnce(open => open())

      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('click')

      expect(setTimeoutSpy).toHaveBeenCalled()
      expect(open).toHaveBeenCalled()
      isIeOrEdgeSpy.mockClear()
      setTimeoutSpy.mockClear()
    })
  })

  describe('onFocus', () => {
    it('sets focus state', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, isFocused }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              {isFocused && <span className="dropzone-focused" />}
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('focus', { defaultPrevented: false })
      expect(dropzone.find('.dropzone-focused')).toHaveLength(1)
    })

    it('calls user supplied onFocus', async () => {
      const onFocus = jest.fn()
      const dropzone = mount(
        <Dropzone onFocus={onFocus}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('focus', { defaultPrevented: false })
      expect(dropzone.state('isFocused')).toBe(true)
      expect(onFocus).toHaveBeenCalled()
    })

    it('does not set focus state if user supplied onFocus prevented default', async () => {
      const onFocus = jest.fn().mockImplementationOnce(evt => {
        Object.assign(evt, {
          defaultPrevented: true
        })
      })
      const dropzone = mount(
        <Dropzone onFocus={onFocus}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('focus', {})
      expect(onFocus).toHaveBeenCalled()
    })
  })

  describe('onBlur', () => {
    it('unsets focus state', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, isFocused }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              {isFocused && <span className="dropzone-focused" />}
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('focus', { defaultPrevented: false })
      expect(dropzone.find('.dropzone-focused')).toHaveLength(1)
      dropzone.simulate('blur', { defaultPrevented: false })
      expect(dropzone.find('.dropzone-focused')).toHaveLength(0)
    })

    it('calls user supplied onBlur', async () => {
      const onBlur = jest.fn()
      const dropzone = mount(
        <Dropzone onBlur={onBlur}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('blur', { defaultPrevented: false })
      expect(onBlur).toHaveBeenCalled()
    })

    it('does not unset focus state if user supplied onBlur prevented default', async () => {
      const onBlur = jest.fn().mockImplementationOnce(evt => {
        Object.assign(evt, {
          defaultPrevented: true
        })
      })
      const dropzone = mount(
        <Dropzone onBlur={onBlur}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('focus', { defaultPrevented: false })
      dropzone.simulate('blur', {})
      expect(onBlur).toHaveBeenCalled()
    })
  })

  describe('onKeyDown', () => {
    it('opens the file dialog on SPACE/ENTER if component is in focus', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')

      dropzone.simulate('keydown', {
        keyCode: 32,
        defaultPrevented: false,
        preventDefault() {}
      })

      dropzone.simulate('keydown', {
        keyCode: 13,
        defaultPrevented: false,
        preventDefault() {}
      })

      expect(open).toHaveBeenCalledTimes(2)
    })

    it('does not react to keydown if component is disabled', async () => {
      const dropzone = mount(
        <Dropzone disabled>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('keydown', {
        keyCode: 32,
        defaultPrevented: false,
        preventDefault() {}
      })
      expect(open).not.toHaveBeenCalled()
    })

    it('does not react to keydown if component is not in focus', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.find('input').simulate('keydown', {
        keyCode: 32,
        defaultPrevented: false,
        preventDefault() {}
      })
      expect(open).not.toHaveBeenCalled()
    })

    it('does not react to keydown from child components', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.find('input').simulate('keydown', { keyCode: 13 })
      expect(open).not.toHaveBeenCalled()
    })

    it('calls user supplied onKeyDown', async () => {
      const onKeyDown = jest.fn()
      const dropzone = mount(
        <Dropzone onKeyDown={onKeyDown}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('keydown', {
        keyCode: 32,
        defaultPrevented: false,
        preventDefault() {}
      })
      expect(onKeyDown).toHaveBeenCalled()
      expect(open).toHaveBeenCalled()
    })

    it('does not call user supplied onKeyDown if component is not in focus', async () => {
      const onKeyDown = jest.fn()
      const dropzone = mount(
        <Dropzone onKeyDown={onKeyDown}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.find('input').simulate('keydown', {
        keyCode: 32,
        defaultPrevented: false,
        preventDefault() {}
      })
      expect(onKeyDown).not.toHaveBeenCalled()
    })

    it('does not react to keydown if user-supplied onKeyDown prevents default', async () => {
      const onKeyDown = jest.fn().mockImplementationOnce(evt => {
        Object.assign(evt, {
          defaultPrevented: true
        })
      })
      const dropzone = mount(
        <Dropzone onKeyDown={onKeyDown}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('keydown', { keyCode: 32 })
      expect(onKeyDown).toHaveBeenCalled()
      expect(open).not.toHaveBeenCalled()
    })

    it('does not react to other keys', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('keydown', {
        keyCode: 100,
        defaultPrevented: false,
        preventDefault() {}
      })
      expect(open).not.toHaveBeenCalled()
    })
  })

  describe('drag-n-drop', async () => {
    it('should override onDrag* methods', async () => {
      const props = {
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn()
      }
      const component = mount(
        <Dropzone {...props}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await component.simulate('dragStart', createDtWithFiles(files))
      await flushPromises(component)
      expect(props.onDragStart).toHaveBeenCalled()

      await component.simulate('dragEnter', createDtWithFiles(files))
      await flushPromises(component)
      expect(props.onDragEnter).toHaveBeenCalled()

      await component.simulate('dragOver', createDtWithFiles(files))
      await flushPromises(component)
      expect(props.onDragOver).toHaveBeenCalled()

      await component.simulate('dragLeave', createDtWithFiles(files))
      await flushPromises(component)
      expect(props.onDragLeave).toHaveBeenCalled()
    })

    it('should not call onDrag* if there are no files', async () => {
      const props = {
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn(),
        onDrop: jest.fn(),
        onDropAccepted: jest.fn(),
        onDropRejected: jest.fn()
      }

      const component = mount(
        <Dropzone {...props}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await component.simulate('dragStart', createDtWithTextTypes())
      await flushPromises(component)
      expect(props.onDragStart).not.toHaveBeenCalled()

      await component.simulate('dragEnter', createDtWithTextTypes())
      await flushPromises(component)
      expect(props.onDragEnter).not.toHaveBeenCalled()

      await component.simulate('dragOver', createDtWithTextTypes())
      await flushPromises(component)
      expect(props.onDragOver).not.toHaveBeenCalled()

      await component.simulate('dragLeave', createDtWithTextTypes())
      await flushPromises(component)
      expect(props.onDragLeave).not.toHaveBeenCalled()

      await component.simulate('drop', createDtWithTextItems())
      await flushPromises(component)
      expect(props.onDrop).not.toHaveBeenCalled()
      expect(props.onDropAccepted).not.toHaveBeenCalled()
      expect(props.onDropRejected).not.toHaveBeenCalled()
    })

    it('should call onDrag* if the DataTransfer has files but cannot access the data', async () => {
      const props = {
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn(),
        onDrop: jest.fn(),
        onDropAccepted: jest.fn(),
        onDropRejected: jest.fn()
      }

      const component = mount(
        <Dropzone {...props}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await component.simulate('dragStart', createDtWithFiles([]))
      await flushPromises(component)
      expect(props.onDragStart).toHaveBeenCalled()

      await component.simulate('dragEnter', createDtWithFiles([]))
      await flushPromises(component)
      expect(props.onDragEnter).toHaveBeenCalled()

      await component.simulate('dragOver', createDtWithFiles([]))
      await flushPromises(component)
      expect(props.onDragOver).toHaveBeenCalled()

      await component.simulate('dragLeave', createDtWithFiles([]))
      await flushPromises(component)
      expect(props.onDragLeave).toHaveBeenCalled()

      await component.simulate('drop', createDtWithFiles(files))
      await flushPromises(component)
      expect(props.onDrop).toHaveBeenCalled()
      expect(props.onDropAccepted).toHaveBeenCalledWith(files, expect.any(Object))
      expect(props.onDropRejected).not.toHaveBeenCalled()
    })

    it('should set proper dragActive state on dragEnter', async () => {
      const component = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, ...restProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...restProps} />
            </div>
          )}
        </Dropzone>
      )
      component.simulate('dragEnter', createDtWithFiles(files))

      const updatedDropzone = await flushPromises(component)
      const child = updatedDropzone.find(DummyChildComponent)

      expect(child).toHaveProp('isDragActive', true)
      expect(child).toHaveProp('isDragAccept', true)
      expect(child).toHaveProp('isDragReject', false)
    })

    it('should set proper dragReject state on dragEnter', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*">
          {({ getRootProps, getInputProps, ...restProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...restProps} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('dragEnter', createDtWithFiles(files.concat(images)))
      const updatedDropzone = await flushPromises(dropzone)
      const child = updatedDropzone.find(DummyChildComponent)
      expect(child).toHaveProp('isDragActive', true)
      expect(child).toHaveProp('isDragAccept', false)
      expect(child).toHaveProp('isDragReject', true)
    })

    it('should set proper dragActive state if multiple is false', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*" multiple={false}>
          {({ getRootProps, getInputProps, ...rest }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...rest} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('dragEnter', createDtWithFiles(files))
      const updatedDropzone = await flushPromises(dropzone)
      const child = updatedDropzone.find(DummyChildComponent)
      expect(child).toHaveProp('isDragActive', true)
      expect(child).toHaveProp('isDragAccept', false)
      expect(child).toHaveProp('isDragReject', true)
    })

    it('should set proper dragAccept state if multiple is false', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*" multiple={false}>
          {({ getRootProps, getInputProps, ...rest }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...rest} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('dragEnter', createDtWithFiles(images))
      const updatedDropzone = await flushPromises(dropzone)
      const child = updatedDropzone.find(DummyChildComponent)
      expect(child).toHaveProp('isDragActive', true)
      expect(child).toHaveProp('isDragAccept', true)
      expect(child).toHaveProp('isDragReject', true)
    })

    it('should keep dragging active when leaving from arbitrary node', async () => {
      const arbitraryOverlay = mount(<div />)
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, ...rest }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...rest} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('dragEnter', createDtWithFiles(images))
      dropzone.simulate('dragLeave', { target: arbitraryOverlay })
      expect(dropzone.state('isDragActive')).toBe(true)
      expect(dropzone.state('draggedFiles').length > 0).toBe(true)
    })

    it('should set proper dragActive state if accept prop changes mid-drag', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*">
          {({ getRootProps, getInputProps, ...rest }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...rest} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('dragEnter', createDtWithFiles(images))
      const updatedDropzone = await flushPromises(dropzone)
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragActive', true)
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragAccept', true)
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragReject', false)

      dropzone.setProps({ accept: 'text/*' })
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragActive', true)
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragAccept', false)
      expect(updatedDropzone.find(DummyChildComponent)).toHaveProp('isDragReject', true)
    })

    it('should expose state to children', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*">
          {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              {isDragReject && `${isDragActive && 'Active but'} Reject`}
              {isDragAccept && `${isDragActive && 'Active and'} Accept`}
              {!isDragActive && 'Empty'}
            </div>
          )}
        </Dropzone>
      )
      expect(dropzone.text()).toEqual('Empty')
      await dropzone.simulate('dragEnter', createDtWithFiles(images))
      expect(dropzone.text()).toEqual('Active and Accept')
      await dropzone.simulate('dragEnter', createDtWithFiles(files))
      expect(dropzone.text()).toEqual('Active but Reject')
    })

    it('should reset the dragActive/dragReject state when leaving after a child goes away', async () => {
      const DragActiveComponent = () => <p>Accept</p>
      const ChildComponent = () => <p>Child component content</p>
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              {isDragReject && 'Rejected'}
              {isDragAccept && (
                <DragActiveComponent isDragAccept={isDragAccept} isDragReject={isDragReject} />
              )}
              {!isDragActive && (
                <ChildComponent isDragAccept={isDragAccept} isDragReject={isDragReject} />
              )}
            </div>
          )}
        </Dropzone>
      )
      const child = dropzone.find(ChildComponent)
      child.simulate('dragEnter', createDtWithFiles(files))
      await dropzone.simulate('dragEnter', createDtWithFiles(files))
      // make sure we handle any duplicate dragEnter events that the browser may send us
      await dropzone.simulate('dragEnter', createDtWithFiles(files))
      const dragActiveChild = dropzone.find(DragActiveComponent)
      expect(dragActiveChild).toExist()
      expect(dragActiveChild).toHaveProp('isDragAccept', true)
      expect(dragActiveChild).toHaveProp('isDragReject', false)

      await dropzone.simulate('dragLeave', createDtWithFiles(files))
      expect(dropzone.find(DragActiveComponent).children()).toHaveLength(0)
      expect(dropzone.find(ChildComponent)).toHaveProp('isDragAccept', false)
      expect(dropzone.find(ChildComponent)).toHaveProp('isDragReject', false)
    })
  })

  describe('open() fn', () => {
    it('should be exposed to children', () => {
      const subject = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, open }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <button type="button" onClick={open}>
                Open
              </button>
            </div>
          )}
        </Dropzone>
      )

      const click = jest.spyOn(subject.instance().input, 'click')
      subject.find('button').simulate('click')

      expect(click).toHaveBeenCalled()
    })

    it('should do nothing if the <input> is missing', () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, open }) => (
            <div {...getRootProps()}>
              <button type="button" onClick={open}>
                Open
              </button>
            </div>
          )}
        </Dropzone>
      )

      const fn = () => dropzone.find('button').simulate('click')
      expect(fn).not.toThrow()
    })
  })

  it('invokes onDop cb when native file section occurs', async () => {
    const props = {
      onDrop: jest.fn(),
      onDropAccepted: jest.fn(),
      onDropRejected: jest.fn()
    }

    const component = mount(
      <Dropzone {...props}>
        {({ getRootProps, getInputProps }) => (
          <div {...getRootProps()}>
            <input {...getInputProps()} />
          </div>
        )}
      </Dropzone>
    )

    const input = component.find('input')
    Object.defineProperty(input, 'files', { value: files })
    const evt = {
      target: input,
      preventDefault() {},
      isPropagationStopped: () => false,
      persist() {}
    }
    input.props().onChange(evt)

    await flushPromises(component)

    expect(props.onDrop).toHaveBeenCalledWith(evt.target.files, [], evt)
    expect(props.onDropAccepted).toHaveBeenCalledWith(evt.target.files, evt)
    expect(props.onDropRejected).not.toHaveBeenCalled()
  })

  describe('onDrop', () => {
    const expectedEvent = expect.anything()
    const onDrop = jest.fn()
    const onDropAccepted = jest.fn()
    const onDropRejected = jest.fn()

    it('should update the acceptedFiles/rejectedFiles state', async () => {
      let dropzone = mount(
        <Dropzone accept="image/*">
          {({ getRootProps, getInputProps, ...restProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...restProps} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('drop', createDtWithFiles(files))
      dropzone = await flushPromises(dropzone)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('acceptedFiles', [])
      expect(dropzone.find(DummyChildComponent)).toHaveProp('rejectedFiles', files)

      dropzone.simulate('drop', createDtWithFiles(images))
      dropzone = await flushPromises(dropzone)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('acceptedFiles', images)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('rejectedFiles', [])
    })

    it('should reset the dragActive/dragReject state', async () => {
      let dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps, ...restProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...restProps} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.simulate('dragEnter', createDtWithFiles(files))
      dropzone = await flushPromises(dropzone)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('isDragActive', true)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('isDragReject', false)
      dropzone.simulate('drop', createDtWithFiles(files))
      dropzone = await flushPromises(dropzone)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('isDragActive', false)
      expect(dropzone.find(DummyChildComponent)).toHaveProp('isDragReject', false)
    })

    it('should reject invalid file when multiple is false', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*" onDrop={onDrop} multiple={false}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], files, expectedEvent)
    })

    it('should allow single files to be dropped if multiple is false', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*" onDrop={onDrop} multiple={false}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles([images[0]]))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([images[0]], [], expectedEvent)
    })

    it('should reject multiple files to be dropped if multiple is false', async () => {
      const dropzone = mount(
        <Dropzone accept="image/*" onDrop={onDrop} multiple={false}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], images, expectedEvent)
    })

    it('should take all dropped files if multiple is true', async () => {
      const dropzone = mount(
        <Dropzone onDrop={onDrop} multiple>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(images, [], expectedEvent)
    })

    it('should set this.isFileDialogActive to false', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.instance().isFileDialogActive = true
      await dropzone.simulate('drop', createDtWithFiles(files))
      expect(dropzone.instance().isFileDialogActive).toEqual(false)
    })

    it('should always call onDrop callback with accepted and rejected arguments', async () => {
      const dropzone = mount(
        <Dropzone onDrop={onDrop} accept="image/*">
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], files, expectedEvent)
      onDrop.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(images, [], expectedEvent)
      onDrop.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(files.concat(images)))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(images, files, expectedEvent)
    })

    it('should call onDropAccepted callback if some files were accepted', async () => {
      const dropzone = mount(
        <Dropzone onDropAccepted={onDropAccepted} accept="image/*">
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDropAccepted).not.toHaveBeenCalled()
      onDropAccepted.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
      onDropAccepted.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(files.concat(images)))
      await flushPromises(dropzone)
      expect(onDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
    })

    it('should call onDropRejected callback if some files were rejected', async () => {
      const dropzone = mount(
        <Dropzone onDropRejected={onDropRejected} accept="image/*">
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDropRejected).not.toHaveBeenCalled()
      onDropRejected.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDropRejected).toHaveBeenCalledWith(files, expectedEvent)
      onDropRejected.mockClear()

      await dropzone.simulate('drop', createDtWithFiles(files.concat(images)))
      await flushPromises(dropzone)
      expect(onDropRejected).toHaveBeenCalledWith(files, expectedEvent)
    })

    it('applies the accept prop to the dropped files', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          accept="image/*"
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], files, expectedEvent)
      expect(onDropAccepted).not.toHaveBeenCalled()
      expect(onDropRejected).toHaveBeenCalledWith(files, expectedEvent)
    })

    it('applies the accept prop to the dropped images', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          accept="image/*"
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(images, [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })

    it('accepts a dropped image when Firefox provides a bogus file type', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          accept="image/*"
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const bogusImages = [createFile('bogus.gif', 1234, 'application/x-moz-file')]

      await dropzone.simulate('drop', createDtWithFiles(bogusImages))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(bogusImages, [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(bogusImages, expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })

    it('accepts all dropped files and images when no accept prop is specified', async () => {
      const dropzone = mount(
        <Dropzone onDrop={onDrop} onDropAccepted={onDropAccepted} onDropRejected={onDropRejected}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(files.concat(images)))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(files.concat(images), [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(files.concat(images), expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })

    it('applies the maxSize prop to the dropped files', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          maxSize={1111}
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(files, [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(files, expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })

    it('applies the maxSize prop to the dropped images', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          maxSize={1111}
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], images, expectedEvent)
      expect(onDropAccepted).not.toHaveBeenCalled()
      expect(onDropRejected).toHaveBeenCalledWith(images, expectedEvent)
    })

    it('applies the minSize prop to the dropped files', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          minSize={1112}
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith([], files, expectedEvent)
      expect(onDropAccepted).not.toHaveBeenCalled()
      expect(onDropRejected).toHaveBeenCalledWith(files, expectedEvent)
    })

    it('applies the minSize prop to the dropped images', async () => {
      const dropzone = mount(
        <Dropzone
          onDrop={onDrop}
          onDropAccepted={onDropAccepted}
          onDropRejected={onDropRejected}
          minSize={1112}
        >
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(images))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(images, [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })

    it('accepts all dropped files and images when no size prop is specified', async () => {
      const dropzone = mount(
        <Dropzone onDrop={onDrop} onDropAccepted={onDropAccepted} onDropRejected={onDropRejected}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      await dropzone.simulate('drop', createDtWithFiles(files.concat(images)))
      await flushPromises(dropzone)
      expect(onDrop).toHaveBeenCalledWith(files.concat(images), [], expectedEvent)
      expect(onDropAccepted).toHaveBeenCalledWith(files.concat(images), expectedEvent)
      expect(onDropRejected).not.toHaveBeenCalled()
    })
  })

  describe('onCancel', () => {
    beforeEach(() => {
      jest.useFakeTimers(true)
    })

    afterEach(() => {
      jest.useFakeTimers(false)
    })

    it('should not invoke onFileDialogCancel everytime window receives focus', () => {
      const onFileDialogCancel = jest.fn()
      mount(
        <Dropzone onFileDialogCancel={onFileDialogCancel}>
          {({ getRootProps, getInputProps }) => (
            <div id="on-cancel-example" {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      // Simulated DOM event - onfocus
      document.body.addEventListener('focus', () => {})
      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('focus', false, true)
      document.body.dispatchEvent(evt)
      jest.runAllTimers()
      expect(onFileDialogCancel).not.toHaveBeenCalled()
    })

    it('should not invoke onFileDialogCancel if input does not exist', () => {
      const onFileDialogCancel = jest.fn()
      mount(
        <Dropzone onFileDialogCancel={onFileDialogCancel}>
          {({ getRootProps }) => <div id="on-cancel-example" {...getRootProps()} />}
        </Dropzone>
      )

      document.body.addEventListener('focus', () => {})
      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('focus', false, true)
      document.body.dispatchEvent(evt)
      jest.runAllTimers()
      expect(onFileDialogCancel).not.toHaveBeenCalled()
    })

    it('should invoke onFileDialogCancel when window receives focus via cancel button and there were no files selected', () => {
      const onFileDialogCancel = jest.fn()
      const component = mount(
        <Dropzone onFileDialogCancel={onFileDialogCancel}>
          {({ getRootProps, getInputProps }) => (
            <div className="dropzone-content" {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      // Test / invoke the click event
      const open = jest.spyOn(component.instance(), 'open')
      component.simulate('click')

      expect(open).toHaveBeenCalled()

      // Simulated DOM event - onfocus
      window.addEventListener('focus', () => {})
      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('focus', false, true)
      window.dispatchEvent(evt)

      jest.runAllTimers()
      expect(onFileDialogCancel).toHaveBeenCalled()
    })

    it('should not invoke onFileDialogCancel when window receives focus via cancel button and there were files selected', () => {
      const onFileDialogCancel = jest.fn()
      const component = mount(
        <Dropzone onFileDialogCancel={onFileDialogCancel}>
          {({ getRootProps, getInputProps }) => (
            <div className="dropzone-content" {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      // Test / invoke the click event
      const open = jest.spyOn(component.instance(), 'open')
      component.simulate('click')

      expect(open).toHaveBeenCalled()

      const input = component.find('input').getDOMNode()

      Object.defineProperty(input, 'files', {
        value: images
      })

      // Simulated DOM event - onfocus
      window.addEventListener('focus', () => {})
      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('focus', false, true)
      window.dispatchEvent(evt)

      jest.runAllTimers()
      expect(onFileDialogCancel).not.toHaveBeenCalled()
    })

    it('should restore isFileDialogActive to false after the FileDialog was closed', () => {
      const component = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      component.simulate('click')

      expect(component.instance().isFileDialogActive).toEqual(true)

      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('focus', false, true)
      window.dispatchEvent(evt)

      jest.runAllTimers()
      expect(component.instance().isFileDialogActive).toEqual(false)
    })
  })

  describe('nested Dropzone component behavior', () => {
    const expectedEvent = expect.anything()
    const onOuterDrop = jest.fn()
    const onOuterDropAccepted = jest.fn()
    const onOuterDropRejected = jest.fn()

    const onInnerDrop = jest.fn()
    const onInnerDropAccepted = jest.fn()
    const onInnerDropRejected = jest.fn()

    const InnerDragAccepted = () => <p>Accepted</p>
    const InnerDragRejected = () => <p>Rejected</p>
    const InnerDropzone = () => (
      <Dropzone
        onDrop={onInnerDrop}
        onDropAccepted={onInnerDropAccepted}
        onDropRejected={onInnerDropRejected}
        accept="image/*"
      >
        {({ getRootProps, getInputProps, isDragAccept, isDragReject, isDragActive }) => (
          <div {...getRootProps()}>
            <input {...getInputProps()} />
            {isDragReject && <InnerDragRejected />}
            {isDragAccept && <InnerDragAccepted />}
            {!isDragActive && <p>No drag</p>}
          </div>
        )}
      </Dropzone>
    )

    describe('dropping on the inner dropzone', () => {
      it('does dragEnter on both dropzones', async () => {
        const outerDropzone = mount(
          <Dropzone accept="image/*">
            {({ getRootProps, getInputProps, ...restProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                <InnerDropzone {...restProps} />
              </div>
            )}
          </Dropzone>
        )
        outerDropzone.find(InnerDropzone).simulate('dragEnter', createDtWithFiles(images))
        const updatedOuterDropzone = await flushPromises(outerDropzone)
        const innerDropzone = updatedOuterDropzone.find(InnerDropzone)

        expect(innerDropzone).toHaveProp('isDragActive', true)
        expect(innerDropzone).toHaveProp('isDragReject', false)
        expect(innerDropzone.find(InnerDragAccepted)).toExist()
        expect(innerDropzone.find(InnerDragRejected)).not.toExist()
      })

      it('accepts the drop on the inner dropzone', async () => {
        const outerDropzone = mount(
          <Dropzone
            onDrop={onOuterDrop}
            onDropAccepted={onOuterDropAccepted}
            onDropRejected={onOuterDropRejected}
            accept="image/*"
          >
            {({ getRootProps, getInputProps, ...restProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                <InnerDropzone {...restProps} />
              </div>
            )}
          </Dropzone>
        )

        outerDropzone.find(InnerDropzone).simulate('drop', createDtWithFiles(files.concat(images)))
        const updatedOuterDropzone = await flushPromises(outerDropzone)
        const innerDropzone = updatedOuterDropzone.find(InnerDropzone)

        expect(onInnerDrop).toHaveBeenCalledTimes(1)
        expect(onInnerDrop).toHaveBeenCalledWith(images, files, expectedEvent)
        expect(onInnerDropAccepted).toHaveBeenCalledTimes(1)
        expect(onInnerDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
        expect(onInnerDropRejected).toHaveBeenCalledTimes(1)
        expect(onInnerDropRejected).toHaveBeenCalledWith(files, expectedEvent)

        expect(innerDropzone.find(InnerDragAccepted)).not.toExist()
        expect(innerDropzone.find(InnerDragRejected)).not.toExist()
      })

      it('also accepts the drop on the outer dropzone', async () => {
        const outerDropzone = mount(
          <Dropzone
            onDrop={onOuterDrop}
            onDropAccepted={onOuterDropAccepted}
            onDropRejected={onOuterDropRejected}
            accept="image/*"
          >
            {({ getRootProps, getInputProps, ...restProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                <InnerDropzone {...restProps} />
              </div>
            )}
          </Dropzone>
        )

        outerDropzone.simulate('drop', createDtWithFiles(files.concat(images)))
        const updatedOuterDropzone = await flushPromises(outerDropzone)

        const innerDropzone = updatedOuterDropzone.find(InnerDropzone)

        expect(onOuterDrop).toHaveBeenCalledTimes(1)
        expect(onOuterDrop).toHaveBeenCalledWith(images, files, expectedEvent)
        expect(onOuterDropAccepted).toHaveBeenCalledTimes(1)
        expect(onOuterDropAccepted).toHaveBeenCalledWith(images, expectedEvent)
        expect(onOuterDropRejected).toHaveBeenCalledTimes(1)
        expect(onOuterDropRejected).toHaveBeenCalledWith(files, expectedEvent)
        expect(innerDropzone).toHaveProp('isDragActive', false)
        expect(innerDropzone).toHaveProp('isDragReject', false)
      })

      it('does not invoke any drag event cbs on parent if child stopped event propagation', async () => {
        const parentProps = {
          onDragEnter: jest.fn(),
          onDragOver: jest.fn(),
          onDragLeave: jest.fn(),
          onDrop: jest.fn()
        }

        const InnerDropzone = () => (
          <Dropzone
            onDragEnter={evt => evt.stopPropagation()}
            onDragOver={evt => evt.stopPropagation()}
            onDragLeave={evt => evt.stopPropagation()}
            onDrop={(accepted, rejected, evt) => evt.stopPropagation()}
          >
            {({ getRootProps, getInputProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
              </div>
            )}
          </Dropzone>
        )

        const outerDropzone = mount(
          <Dropzone {...parentProps}>
            {({ getRootProps, getInputProps, ...restProps }) => (
              <div {...getRootProps()}>
                <input {...getInputProps()} />
                <InnerDropzone {...restProps} />
              </div>
            )}
          </Dropzone>
        )

        outerDropzone.find(InnerDropzone).simulate('dragEnter', createDtWithFiles())
        await flushPromises(outerDropzone)

        outerDropzone.find(InnerDropzone).simulate('dragOver', createDtWithFiles())
        await flushPromises(outerDropzone)

        outerDropzone.find(InnerDropzone).simulate('dragLeave', createDtWithFiles())
        await flushPromises(outerDropzone)

        outerDropzone.find(InnerDropzone).simulate('drop', createDtWithFiles(images))
        await flushPromises(outerDropzone)

        expect(parentProps.onDragEnter).not.toHaveBeenCalled()
        expect(parentProps.onDragOver).not.toHaveBeenCalled()
        expect(parentProps.onDragLeave).not.toHaveBeenCalled()
        expect(parentProps.onDrop).not.toHaveBeenCalled()
      })
    })
  })

  describe('behavior', () => {
    it('does not throw an error when html is dropped instead of files and multiple is false', () => {
      const dropzone = mount(
        <Dropzone multiple={false}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      const fn = () => dropzone.simulate('drop', createDtWithFiles([]))
      expect(fn).not.toThrow()
    })

    it('does not allow actions when disabled props is true', () => {
      const dropzone = mount(
        <Dropzone disabled>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      const open = jest.spyOn(dropzone.instance(), 'open')
      dropzone.simulate('click')
      expect(open).not.toHaveBeenCalled()
    })

    it('when toggle disabled props, Dropzone works as expected', () => {
      const dropzone = mount(
        <Dropzone disabled>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      const open = jest.spyOn(dropzone.instance(), 'open')

      dropzone.setProps({ disabled: false })

      dropzone.simulate('click')
      expect(open).toHaveBeenCalled()
    })

    it('should not set state after onDrop callbacks', async () => {
      let setState
      const onDrop = () => {
        setState.mockClear()
      }
      let dropzone = mount(
        <Dropzone accept="image/*" onDrop={onDrop}>
          {({ getRootProps, getInputProps, ...rest }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <DummyChildComponent {...rest} />
            </div>
          )}
        </Dropzone>
      )
      setState = jest.spyOn(dropzone.instance(), 'setState')
      await dropzone.simulate('drop', createDtWithFiles(images))
      dropzone = await flushPromises(dropzone)
      expect(setState).not.toHaveBeenCalled()
    })

    it('sets {tabindex} to 0 if the component is not disabled', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      expect(
        dropzone
          .children()
          .first()
          .prop('tabIndex')
      ).toBe(0)
    })

    it('sets {tabindex} to -1 if the component is disabled', async () => {
      const dropzone = mount(
        <Dropzone>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )
      dropzone.setProps({ disabled: true })
      expect(
        dropzone
          .children()
          .first()
          .prop('tabIndex')
      ).toBe(-1)
    })
  })

  describe('plugin integration', () => {
    it('uses the provided plugin fn for getting the files', async () => {
      const props = {
        getDataTransferItems: evt => fromEvent(evt),
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn(),
        onDrop: jest.fn()
      }

      const dropzone = mount(
        <Dropzone {...props}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      const data = JSON.stringify({ ping: true })
      const file = new File([data], name, {
        type: 'application/json'
      })
      const files = [file]

      await dropzone.simulate('dragStart', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(props.onDragStart).toHaveBeenCalled()

      await dropzone.simulate('dragEnter', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(props.onDragEnter).toHaveBeenCalled()

      await dropzone.simulate('dragOver', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(props.onDragOver).toHaveBeenCalled()

      await dropzone.simulate('dragLeave', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(props.onDragLeave).toHaveBeenCalled()

      await dropzone.simulate('drop', createDtWithFiles(files))
      await flushPromises(dropzone)
      expect(props.onDrop).toHaveBeenCalled()

      const [call] = props.onDrop.mock.calls
      const [fileList] = call

      expect(fileList).toHaveLength(files.length)

      const [item] = fileList

      expect(item.name).toEqual(file.name)
      expect(item.size).toEqual(file.size)
      expect(item.type).toEqual(file.type)
      expect(item.lastModified).toEqual(file.lastModified)
    })

    it('ignores the plugin result if it does not comply with the expected type signature', async () => {
      const props = {
        getDataTransferItems: evt => Promise.resolve(evt.dataTransfer.items),
        onDragStart: jest.fn(),
        onDragEnter: jest.fn(),
        onDragOver: jest.fn(),
        onDragLeave: jest.fn(),
        onDrop: jest.fn()
      }

      const dropzone = mount(
        <Dropzone {...props}>
          {({ getRootProps, getInputProps }) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
            </div>
          )}
        </Dropzone>
      )

      const items = [
        {
          kind: 'string',
          type: 'text/plain',
          getAsFile() {
            return null
          }
        }
      ]
      const types = ['text/plain']

      await dropzone.simulate('dragStart', createDtWithItems(items, types))
      await flushPromises(dropzone)
      expect(props.onDragStart).not.toHaveBeenCalled()

      await dropzone.simulate('dragEnter', createDtWithItems(items, types))
      await flushPromises(dropzone)
      expect(props.onDragEnter).not.toHaveBeenCalled()

      await dropzone.simulate('dragOver', createDtWithItems(items, types))
      await flushPromises(dropzone)
      expect(props.onDragOver).not.toHaveBeenCalled()

      await dropzone.simulate('dragLeave', createDtWithItems(items, types))
      await flushPromises(dropzone)
      expect(props.onDragLeave).not.toHaveBeenCalled()

      await dropzone.simulate('drop', createDtWithItems(items, types))
      await flushPromises(dropzone)
      expect(props.onDrop).not.toHaveBeenCalled()
    })
  })
})
