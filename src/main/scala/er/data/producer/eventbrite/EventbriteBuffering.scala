package er.data.producer.eventbrite

import cats.effect.{ConcurrentEffect, Timer}
import er.data.producer.Logger
import er.data.producer.eventbrite.EventbriteService._
import fs2.Stream
import fs2.concurrent.Queue
import cats.implicits._

import scala.concurrent.duration._

class EventbriteBuffering[F[_]](q: Queue[F, EventbriteEvent], callF: Int => F[EventbriteResponse])
                               (implicit L: Logger[F], C: ConcurrentEffect[F],
                                T: Timer[F]) {

  def start: Stream[F, Unit] = {

    def processPage(page: Int): F[EventbriteResponse] = for {
      _ <- L.logInfo(s"Calling page: $page")
      resp <- callF(page)
    } yield resp

    def loop(resp: EventbriteResponse): F[Unit] = resp match {
      case SuccessEbResponse(ls, pagination) => for {
        _ <- Stream.emits(ls).covary[F]
          .mapAsyncUnordered(4)(q.enqueue1)
          .compile.drain
        _ <- if (pagination.moreItems) for {
          _ <- T.sleep(1.second)
          resp <- processPage(pagination.pageNumber + 1)
          _ <- loop(resp)
        } yield ()
        else C.unit
      } yield ()
      case FailedEbResponse(status, details) => L.logError(s"Eventbrite error. Status: $status. Details: $details")
      case ParsingResponseFailure(failure) => L.logError(s"Couldn't parse response. ${failure.getMessage}.", failure)
      case InvalidUrl => L.logError("Invalid request url.")
    }

    val init: F[Unit] = for {
      resp <- processPage(1)
      _ <- loop(resp)
    } yield ()

    Stream.eval(init)
  }

}
