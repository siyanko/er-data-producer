package er.data.producer

import cats.effect.{ConcurrentEffect, IO}
import er.data.producer.MuenchenDe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{EntityDecoder, Method, Request, Uri, UrlForm}
import io.circe.generic.auto._
import org.http4s.circe._
import scala.concurrent.ExecutionContext

/*
  Interaction with muenchen.de website.
 */
trait MuenchenDe[F[_]] {
  def get: F[List[MuenchenDeEventData]]
}

object MuenchenDe {

  implicit def ioMuenchenDe(implicit ec: ExecutionContext,
                            ceff: ConcurrentEffect[IO]
                           ): MuenchenDe[IO] = new MuenchenDe[IO] {

    val mkRequest: Request[IO] = Request[IO]()
      .withUri(Uri.uri("https://www.muenchen.de/opendi/ajax.html"))
      .withMethod(Method.POST)
      .withEntity(UrlForm(
        "opComposite" -> "ajax",
        "action" -> "getEventListElements",
        "value" -> "cs---0"
      ))


    override def get: IO[List[MuenchenDeEventData]] =
      BlazeClientBuilder[IO](ec).resource.use(
        _.expect[List[MuenchenDeEventData]](mkRequest)
      )
  }

  final case class GeoLocation(latitude: String, longitude: String)

  final case class Address(streetAddress: String, addressLocality: String, postalCode: String)

  final case class Location(address: Option[Address], geo: Option[GeoLocation])

  final case class MuenchenDeEventData(
                                        etName: String,
                                        timestamp: Option[String],
                                        endTimestamp: Option[String],
                                        extUrl: Option[String],
                                        detailLogoUrl: Option[String],
                                        detailLink: Option[String],
                                        location: Option[Location]
                                      )

  implicit val muenchenDeEventsEntityDecoder: EntityDecoder[IO, List[MuenchenDeEventData]] = jsonOf[IO, List[MuenchenDeEventData]]
  implicit val muenchenDeEventDataEntityDecoder: EntityDecoder[IO, MuenchenDeEventData] = jsonOf[IO, MuenchenDeEventData]
  implicit val addressEntityDecoder: EntityDecoder[IO, Address] = jsonOf[IO, Address]
  implicit val geoLocationEntityDecoder: EntityDecoder[IO, GeoLocation] = jsonOf[IO, GeoLocation]

}
