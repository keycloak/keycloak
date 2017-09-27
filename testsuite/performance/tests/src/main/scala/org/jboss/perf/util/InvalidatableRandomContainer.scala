package org.jboss.perf.util

trait Invalidatable[T] {
  def invalidate(): Boolean
  def apply(): T
}

/**
 * Extends {@link RandomContainer} by storing {@link Invalidatable} entries -
 * when adding an entry an {@link Invalidatable} reference is returned, which could be
 * later removed from the collection in O(1).
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
class InvalidatableRandomContainer[T >: Null <: AnyRef : Manifest](cap: Int = 16)
  extends RandomContainer[Invalidatable[T]](IndexedSeq(), cap) {

  def add(elem: T): Invalidatable[T] = {
    val entry = new Entry(elem)
    this += entry
    entry
  }

  private def remove(entry : Entry): Boolean = this.synchronized {
    if (entry.getPos >= 0) {
      removeInternal(entry.getPos)
      true
    } else false
  }

  protected override def move(elem : Invalidatable[T], from: Int, to: Int): Unit = {
    elem.asInstanceOf[Entry].updatePos(to)
  }

  private class Entry(value : T) extends Invalidatable[T] {
    private var pos: Int = 0

    override def invalidate(): Boolean = {
      remove(this)
    }

    override def apply() = value

    private[InvalidatableRandomContainer] def updatePos(pos: Int): Unit = {
      this.pos = pos
    }

    private[InvalidatableRandomContainer] def getPos = pos
  }

}
