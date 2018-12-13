package er.data.producer.eventbrite

import cats.effect.ConcurrentEffect
import er.data.producer.Logger
import er.data.producer.eventbrite.EventbriteService._
import fs2.concurrent.Queue
import fs2.Stream
import cats.implicits._

class EventbriteBuffering[F[_]](q: Queue[F, EventbriteEvent], callF: F[EventbriteResponse])
                               (implicit L: Logger[F], C: ConcurrentEffect[F]) {

  def start: F[Unit] = {

    val loop: EventbriteResponse => F[Unit] = _ match {
      case SuccessEbResponse(ls, pagination) =>
        Stream.emits(ls).covary[F]
          .mapAsyncUnordered(4)(q.enqueue1)
          .compile.drain
      case FailedEbResponse(status, details) => L.logError(s"Eventbrite error. Status: $status. Details: $details")
      case ParsingResponseFailure(failure) => L.logError(failure.getMessage(), failure)
    }

    val init: F[Unit] = for {
      resp <- callF
      _ <- loop(resp)
    } yield ()

    init
  }

}
