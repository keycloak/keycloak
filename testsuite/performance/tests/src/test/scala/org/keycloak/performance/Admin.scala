package org.keycloak.performance

import io.gatling.core.Predef._
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import java.util.concurrent.CyclicBarrier
import io.gatling.core.structure.ChainBuilder

object Admin extends Admin {

  object Sync { // java.util.concurrent.CyclicBarrier doesn't work with Gatling because of the Akka actor model, need this custom solution

    val usersMax = adminUsers
    val usersArrived = new AtomicInteger(0)
    val latchOpen = new AtomicBoolean(false)
    
    def isLatchOpen = latchOpen.get
    
    def waitForPreviousLatchToCloseIfStillOpen = asLongAs ( s => latchOpen.get && !isGlobalStopRequested, "waitForLatchToCloseTimeout") (
      checkGlobalStopRequest

      .doIfOrElse ( s => (s("waitForLatchToCloseTimeout").as[Int] < 100)) (  // 100 * 100 millis = 10 seconds
        exec { s => 
//          println("previous latch still open, waiting for its closure")
          s
        }.pause(100.millisecond)
      ) ( 
        exec { s => 
          throw new IllegalStateException("Waiting for previous latch to close timed out. This shoulnd't happen.")
          s
        }.exitHereIfFailed
      )
    )
    
    def arriveAtLatchAndOpenIfLast(latchName:String) = exec { s =>
      usersArrived.incrementAndGet
      if (usersArrived.get >= usersMax && !latchOpen.get) {
        println("opening latch: "+latchName)
        latchOpen.set(true) // last arriving opens the latch
      }
      s
    }
    
    def waitForLatchToOpen(timeout:Integer) = asLongAs ( s => !latchOpen.get, "waitForLatchToOpenTimeout") ( 
      checkGlobalStopRequest
      .doIfOrElse ( s => (s("waitForLatchToOpenTimeout").as[Int] < timeout)) ( 
        exec { s => 
//          println("waiting for latch to open. usersArrived: "+usersArrived.get +"/"+usersMax+ ", timeout: "+(timeout - s("waitForLatchToOpenTimeout").as[Int]))
          s
        }.pause(1.second)
      ) ( 
        exec { s => 
          throw new TimeoutException("Waiting for others timed out. Missing users: " + (usersMax - usersArrived.get))
          s
        }.exitHereIfFailed
      )
    )
    
    def leaveLatchAndCloseIfLast(latchName:String) = exec { s => 
      usersArrived.decrementAndGet
      if (usersArrived.get <= 0 && latchOpen.get) {
        println("closing latch: "+latchName)
        latchOpen.set(false) // last leaving closes the latch
      }
      s.remove("waitForLatchToOpenTimeout")
      s
    }
    
    def waitForOthers(latchName:String, timeout:Integer) = exec (
      waitForPreviousLatchToCloseIfStillOpen,
      arriveAtLatchAndOpenIfLast(latchName),
      waitForLatchToOpen(timeout),
      leaveLatchAndCloseIfLast(latchName)
    )
      
    def waitForOthers(latchName:String) : ChainBuilder = waitForOthers(latchName, 10)

    def waitForOthers : ChainBuilder = waitForOthers("")

    
    
    
    val globalStopRequested = new AtomicBoolean(false)
    def isGlobalStopRequested = {
//      println("isGlobalStopRequested: "+globalStopRequested.get)
      globalStopRequested.get
    }
    def checkGlobalStopRequest = exec { s =>
      if (isGlobalStopRequested) throw new RuntimeException("Global stop requested.")
      s
    }.exitHereIfFailed
    
    def requestGlobalStop : ChainBuilder = exec { s =>
      println("Requesting global stop.")
      globalStopRequested.set(true)
      s
    }.exec(checkGlobalStopRequest)
      
  }

}

trait Admin extends OIDC {
  import Admin._

  val adminUsers = TestConfig.numOfWorkers
    
  val adminInjectionProfile = atOnceUsers(adminUsers)

}