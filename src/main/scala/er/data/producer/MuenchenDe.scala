package er.data.producer

import MuenchenDe._
import cats.effect.IO

/*
  Interaction with muenchen.de website.
 */
trait MuenchenDe[F[_]] {
  def get(url: String): F[MuenchenDeEventData]
}

object MuenchenDe {

  implicit val ioMuenchenDe: MuenchenDe[IO] = new MuenchenDe[IO] {
    override def get(url: String): IO[MuenchenDeEventData] = IO(MuenchenDeEventData("test-event"))
  }

  final case class MuenchenDeEventData(eventName: String)

}
