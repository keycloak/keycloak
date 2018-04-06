package org.keycloak.gatling

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.action.{Chainable, UserEnd}
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.result.message.{KO, OK, Status}
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper
import io.gatling.core.validation.{Failure, Success, Validation}

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
object Stopwatch extends StrictLogging {
  @volatile var recording: Boolean = true;
  GatlingActorSystem.instance.registerOnTermination(() => recording = true)

  def apply[T](f: () => T): Result[T] = {
    val start = TimeHelper.nowMillis
    try {
      val result = f()
      Result(Success(result), OK, start, TimeHelper.nowMillis, false)
    } catch {
      case ie: InterruptedException => {
        Result(Failure("Interrupted"), KO, start, start, true)
      }
      case e: Throwable => {
        Stopwatch.log.error("Operation failed with exception", e)
        Result(Failure(e.toString), KO, start, TimeHelper.nowMillis, false)
      }
    }
  }

  def log = logger;
}

case class Result[T](
                      val value: Validation[T],
                      val status: Status,
                      val startTime: Long,
                      val endTime: Long,
                      val interrupted: Boolean
) {
  def check(check: T => Boolean, fail: T => String): Result[T] = {
     value match {
       case Success(v) =>
         if (!check(v)) {
           Result(Failure(fail(v)), KO, startTime, endTime, interrupted);
         } else {
           this
         }
       case _ => this
     }
  }

  def isSuccess =
    value match {
      case Success(_) => true
      case _ => false
    }

  private def record(client: DataWriterClient, session: Session, name: String): Validation[T] = {
    if (!interrupted && Stopwatch.recording) {
      var msg = value match {
        case Failure(m) => Some(m)
        case _ => None
      }
      client.writeRequestData(session, name, startTime, startTime, endTime, endTime, status, msg)
    }
    value
  }

  def recordAndStopOnFailure(client: DataWriterClient with Chainable, session: Session, name: String): Validation[T] = {
    val validation = record(client, session, name)
    validation.onFailure(message => {
        Stopwatch.log.error(s"'${client.self.path.name}', ${session.userId} failed to execute: $message")
        UserEnd.instance ! session.markAsFailed
    })
    validation
  }

  def recordAndContinue(client: DataWriterClient with Chainable, session: Session, name: String): Unit = {
    // can't specify follow function as default arg since it uses another parameter
    recordAndContinue(client, session, name, _ => session);
  }

  def recordAndContinue(client: DataWriterClient with Chainable, session: Session, name: String, follow: T => Session): Unit = {
    // 'follow' intentionally does not get session as arg, since caller site already has the reference
    record(client, session, name) match {
      case Success(value) => try {
        client.next ! follow(value)
      } catch {
        case t: Throwable => {
          Stopwatch.log.error(s"'${client.self.path.name}' failed processing", t)
          UserEnd.instance ! session.markAsFailed
      }
    }
      case Failure(message) => {
        Stopwatch.log.error(s"'${client.self.path.name}' failed to execute: $message")
        UserEnd.instance ! session.markAsFailed
      }
    }
  }
}

