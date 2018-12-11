package er.data.producer.eventbrite

import cats.effect.{ConcurrentEffect, IO}
import org.http4s.{AuthScheme, Credentials, MediaType, Method, Request, Uri, UrlForm}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.FollowRedirect
import org.http4s.headers._

import scala.concurrent.ExecutionContext

trait EventbriteService[F[_]] {
  def getMunichEvents: F[String]
}

object EventbriteService {
  def ioEventbriteService(implicit ec: ExecutionContext, ceff: ConcurrentEffect[IO]): EventbriteService[IO] = new EventbriteService[IO] {

    val mkRequest: Request[IO] = Request[IO]()
      .withUri(Uri.uri("https://www.eventbriteapi.com/v3/events/search"))
      .withMethod(Method.GET)
      .withHeaders(
        Authorization(Credentials.Token(AuthScheme.Bearer, "4IACRMEG7AXPOWH7NAKX")),
        Accept(MediaType.application.json))

    println("RESP: " + mkRequest)

    override def getMunichEvents: IO[String] =
      BlazeClientBuilder[IO](ec).resource.use[String] { cl =>
        val client = FollowRedirect(3)(cl)
        client.expect[String](mkRequest)
      }
  }
}