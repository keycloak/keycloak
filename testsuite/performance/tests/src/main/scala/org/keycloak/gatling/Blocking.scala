package org.keycloak.gatling

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.validation.Success

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
object Blocking {
  GatlingActorSystem.instance.registerOnTermination(() => shutdown())

  private val threadPool = Executors.newCachedThreadPool(new ThreadFactory {
    val counter = new AtomicInteger();

    override def newThread(r: Runnable): Thread =
      new Thread(r, "blocking-thread-" + counter.incrementAndGet())
  })

  def apply(f: () => Unit) = {
    threadPool.execute(new Runnable() {
      override def run = {
        f()
      }
    })
    Success(())
  }

  def shutdown() = {
    threadPool.shutdownNow()
  }
}
