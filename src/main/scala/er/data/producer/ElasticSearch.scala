package er.data.producer

import cats.effect.{ConcurrentEffect, IO}
import io.circe.Json
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Uri}
import org.http4s.circe._

import scala.concurrent.ExecutionContext

/*
  Sends data to elastic search
 */
trait ElasticSearch[F[_]] {
  def send(data: Json): F[Unit]
}

object ElasticSearch {
  def ioElasticSearch(implicit ec: ExecutionContext, ceff: ConcurrentEffect[IO]): ElasticSearch[IO] =
    new ElasticSearch[IO] {

      val mkRequest: Json => Request[IO] = data => Request[IO]()
        .withUri(Uri.uri("http://localhost:9200/events/_doc"))
        .withMethod(Method.POST)
        .withEntity(data)

      override def send(data: Json): IO[Unit] =
        BlazeClientBuilder[IO](ec).resource.use(
          _.expect[String](mkRequest(data))
            .map(_ => ())
        )
    }
}
