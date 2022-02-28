declare function fastq<T>(context: T, worker: fastq.worker<T>, concurrency: number): fastq.queue
declare function fastq<T>(worker: fastq.worker<T>, concurrency: number): fastq.queue

declare namespace fastq {
  type worker<T> = (this: T, arg: any, cb: () => void) => void
  type done = (err: Error, result: any) => void

  interface queue {
    push(task: any, done: done): void
    unshift(task: any, done: done): void
    pause(): any
    resume(): any
    idle(): boolean
    length(): number
    kill(): any
    killAndDrain(): any
    concurrency: number
    drain(): any
    empty: () => void
    saturated: () => void
  }
}

export = fastq