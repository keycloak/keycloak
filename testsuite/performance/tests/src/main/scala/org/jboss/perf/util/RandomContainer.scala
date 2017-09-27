package org.jboss.perf.util

import scala.util.Random

/**
  * Allows O(1) random removals and also adding in O(1), though these
  * operations can be blocked by another thread.
  * {@link #size} is non-blocking.
  *
  * Does not implement any collection interface/trait for simplicity reasons.
  *
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
class RandomContainer[T >: Null <: AnyRef : Manifest](
                         seq: IndexedSeq[T],
                         cap: Int = 0
                         ) {
  private var data = new Array[T](if (cap > 0) cap else seq.length * 2)
  private var takeIndex = 0
  private var putIndex = seq.length
  @volatile private var count = seq.length // volatile as we want to read size without lock

  {
    var pos = 0
    for (e <- seq) {
      data(pos) = e
      pos = pos + 1
    }
  }

  def +=(elem: T): RandomContainer[T] = this.synchronized {
    if (count == data.length) {
      val tmp = new Array[T](data.length * 2)
      for (i <- 0 until data.length) {
        tmp(i) = data(i)
      }
      tmp(data.length) = elem;
      move(elem, -1, data.length)
      putIndex = data.length + 1
      takeIndex = 0
      data = tmp;
    } else {
      data(putIndex) = elem
      move(elem, -1, putIndex)
      putIndex = (putIndex + 1) % data.length
    }
    count += 1
    this
  }

  /**
    * Executed under lock, allows to track position of element
    *
    * @param elem
    * @param from
    * @param to
    */
  protected def move(elem: T, from: Int, to: Int): Unit = {}

  def removeRandom(random: Random): T = this.synchronized {
    if (count == 0) {
      return null;
    }
    removeInternal((takeIndex + random.nextInt(count)) % data.length)
  }

  protected def removeInternal(index: Int): T = {
    assert(count > 0)
    count -= 1
    if (index == takeIndex) {
      val elem = data(takeIndex);
      assert(elem != null)
      move(elem, takeIndex, -1)
      data(takeIndex) = null
      takeIndex = (takeIndex + 1) % data.length
      return elem
    } else {
      val elem = data(index)
      assert(elem != null)
      move(elem, index, -1)
      val moved = data(takeIndex)
      assert(moved != null)
      data(index) = moved
      move(moved, takeIndex, index)
      data(takeIndex) = null // unnecessary
      takeIndex = (takeIndex + 1) % data.length
      return elem
    }
  }

  def size() = count
}
