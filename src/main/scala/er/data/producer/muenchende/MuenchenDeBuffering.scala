package er.data.producer.muenchende

import cats.effect.{ConcurrentEffect, Timer}
import er.data.producer.Logger
import er.data.producer.muenchende.MuenchenDe.MuenchenDeEventData
import fs2.Stream
import fs2.concurrent.Queue
import scala.concurrent.duration._
import cats.implicits._

class MuenchenDeBuffering[F[_]](q: Queue[F, MuenchenDeEventData], callF: Int => F[List[MuenchenDeEventData]], pagesNumber: Int)
                               (implicit L: Logger[F], C: ConcurrentEffect[F],
                                T: Timer[F]) {

  def start: Stream[F, Unit] = {

    val itemsPerPage = 25

    def processPage(page: Int): F[List[MuenchenDeEventData]]= for {
      _ <- L.logInfo(s"Calling page: $page")
      resp <- callF(page * itemsPerPage)
    } yield resp

    def loop(currentPageId: Int, resp: List[MuenchenDeEventData]): F[Unit] = for {
      _ <- Stream.emits(resp).covary[F].to(q.enqueue).compile.drain
      _ <- if (currentPageId + 1 < pagesNumber) for {
        _ <- T.sleep(1.second)
        resp <- processPage(currentPageId + 1)
        _ <- loop(currentPageId + 1, resp)
      } yield ()
      else C.unit
    } yield ()

    val init: F[Unit] = for {
      resp <- processPage(0)
      _ <- loop(0, resp)
    } yield ()

    Stream.eval(init)
  }

}
