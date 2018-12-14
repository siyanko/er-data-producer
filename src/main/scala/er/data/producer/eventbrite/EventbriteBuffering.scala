package er.data.producer.eventbrite

import cats.effect.ConcurrentEffect
import er.data.producer.Logger
import er.data.producer.eventbrite.EventbriteService._
import fs2.Stream
import fs2.concurrent.Queue

class EventbriteBuffering[F[_]](q: Queue[F, EventbriteEvent], callF: Int => F[EventbriteResponse])
                               (implicit L: Logger[F], C: ConcurrentEffect[F]) {

  def start: Stream[F, Unit] = {

    val loop: EventbriteResponse => Stream[F, Unit] = _ match {
      case SuccessEbResponse(ls, pagination) =>
        Stream.emits(ls).covary[F]
          .mapAsyncUnordered(4)(q.enqueue1)
      case FailedEbResponse(status, details) => Stream.eval(L.logError(s"Eventbrite error. Status: $status. Details: $details"))
      case ParsingResponseFailure(failure) => Stream.eval(L.logError(failure.getMessage(), failure))
      case InvalidUrl => Stream.eval(L.logError("Invalid request url."))
    }

    val init: Stream[F, Unit] = for {
      resp <- Stream.eval(callF(1))
      _ <- loop(resp)
    } yield ()

    init
  }

}
