package org.jboss.perf.util

import java.util.UUID

import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.util.Random

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
object Util {
  val random = new Random(1234); // keep fixed seed

  def randomString(length: Int, rand: Random = ThreadLocalRandom.current()): String = {
    val sb = new StringBuilder;
    for (i <- 0 until length) {
      sb.append((rand.nextInt(26) + 'a').toChar)
    }
    sb.toString()
  }

  def randomUUID(rand: Random = ThreadLocalRandom.current()): String =
    new UUID(rand.nextLong(), rand.nextLong()).toString
}
