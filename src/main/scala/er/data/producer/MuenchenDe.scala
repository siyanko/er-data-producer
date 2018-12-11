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
  def get(id: Int): F[List[MuenchenDeEventData]]
}

object MuenchenDe {

  implicit def ioMuenchenDe(implicit ec: ExecutionContext,
                            ceff: ConcurrentEffect[IO]
                           ): MuenchenDe[IO] = new MuenchenDe[IO] {

    val mkRequest: Int => Request[IO] = id => Request[IO]()
      .withUri(Uri.uri("https://www.muenchen.de/opendi/ajax.html"))
      .withMethod(Method.POST)
      .withEntity(UrlForm(
        "opComposite" -> "ajax",
        "action" -> "getEventListElements",
        "value" -> s"cs---$id"
      ))


    override def get(id: Int): IO[List[MuenchenDeEventData]] =
      BlazeClientBuilder[IO](ec).resource.use(
        _.expect[List[MuenchenDeEventData]](mkRequest(id))
      )
  }

  final case class MuenchenDeGeoLocation(latitude: String, longitude: String)

  final case class MuenchenDeAddress(streetAddress: String, addressLocality: String, postalCode: String)

  final case class Location(address: Option[MuenchenDeAddress], geo: Option[MuenchenDeGeoLocation])

  final case class MuenchenDeEventData(
                                        etName: String,
                                        terminIso: Option[String],
                                        endterminIso: Option[String],
                                        extUrl: Option[String],
                                        detailLogoUrl: Option[String],
                                        detailLink: Option[String],
                                        location: Option[Location]
                                      )

  implicit val muenchenDeEventsEntityDecoder: EntityDecoder[IO, List[MuenchenDeEventData]] = jsonOf[IO, List[MuenchenDeEventData]]
  implicit val muenchenDeEventDataEntityDecoder: EntityDecoder[IO, MuenchenDeEventData] = jsonOf[IO, MuenchenDeEventData]
  implicit val addressEntityDecoder: EntityDecoder[IO, MuenchenDeAddress] = jsonOf[IO, MuenchenDeAddress]
  implicit val geoLocationEntityDecoder: EntityDecoder[IO, MuenchenDeGeoLocation] = jsonOf[IO, MuenchenDeGeoLocation]

}
