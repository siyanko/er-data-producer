package er.data.producer.eventbrite

import cats.effect.{ConcurrentEffect, ContextShift, IO}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers._
import org.http4s._

import scala.concurrent.ExecutionContext

trait EventbriteService[F[_]] {
  def getMunichEvents(token: String): F[String]
}

object EventbriteService {
  def ioEventbriteService(implicit ec: ExecutionContext,
                          ceff: ConcurrentEffect[IO]): EventbriteService[IO] = new EventbriteService[IO] {

    val mkRequest: String => Request[IO] = token => Request[IO]()
      .withUri(Uri.uri("https://www.eventbriteapi.com/v3/events/search/?location.address=munich"))
          .withMethod(Method.GET)
          .withHeaders(
            Authorization(Credentials.Token(AuthScheme.Bearer, token)),
            Accept(MediaType.application.json)
          )

    override def getMunichEvents(token: String): IO[String] =
      BlazeClientBuilder[IO](ec).resource.use[String](
            _.fetch[String](mkRequest(token))(_.bodyAsText.compile.toList.map(_.mkString))
          )
  }
}