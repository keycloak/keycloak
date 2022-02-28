import * as fastq from '../'

const queue = fastq({ hello: 'world' }, worker, 1)

queue.push(42, (err, done) => {
  if (err) throw err
  console.log('the result is', done)
})

queue.concurrency

queue.drain()

queue.empty = () => undefined

queue.idle()

queue.kill()

queue.killAndDrain()

queue.length

queue.pause()

queue.resume()

queue.saturated = () => undefined

queue.unshift(42, (err, done) => {
  if (err) throw err
  console.log('the result is', done)
})

function worker(arg: any, cb: any) {
  cb(null, 42 * 2)
}
