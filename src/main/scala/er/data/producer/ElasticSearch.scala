package er.data.producer

import cats.effect.IO
import io.circe.Json

/*
  Sends data to elastic search
 */
trait ElasticSearch [F[_]]{
  def send(data: Json): F[Unit]
}

object ElasticSearch {
  implicit val ioElasticSearch: ElasticSearch[IO] = new ElasticSearch[IO] {
    override def send(data: Json): IO[Unit] = IO(println("Sending data to elastic search: " + data.toString()))
  }
}
