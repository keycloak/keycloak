import {
  isIeOrEdge,
  isKindFile,
  isDragDataWithFiles,
  composeEventHandlers,
  isPropagationStopped,
  isDefaultPrevented
} from './'

describe('isIeOrEdge', () => {
  it('should return true for IE10', () => {
    const userAgent =
      'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0; .NET4.0E; .NET4.0C; .NET CLR 3.5.30729; .NET CLR 2.0.50727; .NET CLR 3.0.30729)'

    expect(isIeOrEdge(userAgent)).toBe(true)
  })

  it('should return true for IE11', () => {
    const userAgent =
      'Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; .NET CLR 2.0.50727; .NET CLR 3.0.30729; .NET CLR 3.5.30729; rv:11.0) like Gecko'
    expect(isIeOrEdge(userAgent)).toBe(true)
  })

  it('should return true for Edge', () => {
    const userAgent =
      'Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16258'

    expect(isIeOrEdge(userAgent)).toBe(true)
  })

  it('should return false for Chrome', () => {
    const userAgent =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36'

    expect(isIeOrEdge(userAgent)).toBe(false)
  })
})

describe('isKindFile()', () => {
  it('should return true for DataTransferItem of kind "file"', () => {
    expect(isKindFile({ kind: 'file' })).toBe(true)
    expect(isKindFile({ kind: 'text/html' })).toBe(false)
    expect(isKindFile({})).toBe(false)
    expect(isKindFile(null)).toBe(false)
  })
})

describe('isPropagationStopped()', () => {
  const trueFn = jest.fn(() => true)

  it('should return result of isPropagationStopped() if isPropagationStopped exists', () => {
    expect(isPropagationStopped({ isPropagationStopped: trueFn })).toBe(true)
  })

  it('should return value of cancelBubble if isPropagationStopped doesnt exist and cancelBubble exists', () => {
    expect(isPropagationStopped({ cancelBubble: true })).toBe(true)
  })

  it('should return false if isPropagationStopped and cancelBubble are missing', () => {
    expect(isPropagationStopped({})).toBe(false)
  })
})

describe('isDefaultPrevented()', () => {
  const trueFn = jest.fn(() => true)

  it('should return value of defaultPrevented if defaultPrevented exists', () => {
    expect(isDefaultPrevented({ defaultPrevented: true })).toBe(true)
  })

  it('should return result of isDefaultPrevented() if isDefaultPrevented exists and defaultPrevented is missing', () => {
    expect(isDefaultPrevented({ isDefaultPrevented: trueFn })).toBe(true)
  })

  it('should return false if isDefaultPrevented and defaultPrevented are missing', () => {
    expect(isDefaultPrevented({})).toBe(false)
  })
})

describe('isDragDataWithFiles()', () => {
  it('should return true if every dragged type is a file', () => {
    expect(isDragDataWithFiles({ dataTransfer: { types: ['Files'] } })).toBe(true)
    expect(isDragDataWithFiles({ dataTransfer: { types: ['application/x-moz-file'] } })).toBe(true)
    expect(
      isDragDataWithFiles({
        dataTransfer: { types: ['Files', 'application/x-moz-file'] }
      })
    ).toBe(true)
    expect(isDragDataWithFiles({ dataTransfer: { types: ['text/plain'] } })).toBe(false)
    expect(isDragDataWithFiles({ dataTransfer: { types: ['text/html'] } })).toBe(false)
    expect(isDragDataWithFiles({ dataTransfer: { types: ['Files', 'application/test'] } })).toBe(
      true
    )
    expect(
      isDragDataWithFiles({
        dataTransfer: { types: ['application/x-moz-file', 'application/test'] }
      })
    ).toBe(true)
  })

  it('should return true if {dataTransfer} is not defined', () => {
    expect(isDragDataWithFiles({})).toBe(true)
  })
})

describe('composeEventHandlers', () => {
  it('returns a fn', () => {
    const fn = composeEventHandlers(() => {})
    expect(typeof fn).toBe('function')
  })

  it('runs every passed fn in order', () => {
    const fn1 = jest.fn()
    const fn2 = jest.fn()
    const fn = composeEventHandlers(fn1, fn2)
    const evt = { type: 'click' }
    const data = { ping: true }
    fn(evt, data)
    expect(fn1).toHaveBeenCalledWith(evt, data)
    expect(fn2).toHaveBeenCalledWith(evt, data)
  })

  it('stops after first fn that calls preventDefault()', () => {
    const fn1 = jest.fn().mockImplementation(evt => {
      Object.defineProperty(evt, 'defaultPrevented', { value: true })
      return evt
    })
    const fn2 = jest.fn()
    const fn = composeEventHandlers(fn1, fn2)
    const evt = new MouseEvent('click')
    fn(evt)
    expect(fn1).toHaveBeenCalledWith(evt)
    expect(fn2).not.toHaveBeenCalled()
  })
})
