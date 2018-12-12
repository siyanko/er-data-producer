package er.data.producer.eventbrite

import cats.effect.{ConcurrentEffect, IO}
import er.data.producer.eventbrite.EventbriteService._
import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor}
import org.http4s.Status._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers._
import SuccessEbResponse._
import Pagination._
import EventbriteEvent._
import EventbriteOrganizer._
import EventbriteVenue._

import scala.concurrent.ExecutionContext

trait EventbriteService[F[_]] {
  def getMunichEvents(token: String): F[EventbriteResponse]
}

object EventbriteService {
  def ioEventbriteService(implicit ec: ExecutionContext,
                          ceff: ConcurrentEffect[IO]): EventbriteService[IO] = new EventbriteService[IO] {

    val mkRequest: String => Request[IO] = token => Request[IO]()
      .withUri(Uri.uri("https://www.eventbriteapi.com/v3/events/search/?location.address=munich&expand=organizer,venue"))
      .withMethod(Method.GET)
      .withHeaders(
        Authorization(Credentials.Token(AuthScheme.Bearer, token)),
        Accept(MediaType.application.json)
      )

    override def getMunichEvents(token: String): IO[EventbriteResponse] =
      BlazeClientBuilder[IO](ec).resource.use[EventbriteResponse](
        _.fetch[EventbriteResponse](mkRequest(token)) { resp =>
          resp.status match {
            case Ok => resp.attemptAs[SuccessEbResponse](jsonOf[IO, SuccessEbResponse]).valueOr(ParsingResponseFailure)
            case status => resp.bodyAsText.compile.toList.map(_.mkString).map(s => FailedEbResponse(status, s))
          }
        }
      )
  }

  final case class EventbriteVenue(name: String,
                                   latitude: String,
                                   longitude: String,
                                   address: String)
  object EventbriteVenue {
    implicit val eventbriteVenueDecoder: Decoder[EventbriteVenue] = new Decoder[EventbriteVenue] {
      override def apply(c: HCursor): Result[EventbriteVenue] = for {
        name <- c.downField("name").as[String]
        latitude <- c.downField("latitude").as[String]
        longitude <- c.downField("longitude").as[String]
        address <- c.downField("address").downField("localized_address_display").as[String]
      } yield EventbriteVenue(name, latitude, longitude, address)
    }
  }

  final case class EventbriteOrganizer(name: Option[String],
                                       description: Option[String],
                                       logoUrl: Option[String],
                                       url: Option[String])
  object EventbriteOrganizer{
    implicit val eventbriteOrganizerDecoder: Decoder[EventbriteOrganizer] = new Decoder[EventbriteOrganizer] {
      override def apply(c: HCursor): Result[EventbriteOrganizer] = for {
        name <- c.downField("name").as[Option[String]]
        description <- c.downField("description").right.downField("text").as[Option[String]]
        logoUrl <- c.downField("logo").right.downField("url").as[Option[String]]
        url <- c.downField("url").as[Option[String]]
      } yield EventbriteOrganizer(name, description, logoUrl, url)
    }
  }

  final case class EventbriteEvent(name: String,
                                   description: Option[String],
                                   url: String,
                                   beginDateTimeUtc: String,
                                   endDateTimeUtc: Option[String],
                                   status: String,
                                   onlineEvent: Boolean,
                                   isSeries: Boolean,
                                   free: Boolean,
                                   logoUrl: String,
                                   organizer: Option[EventbriteOrganizer],
                                   venue: Option[EventbriteVenue])
  object EventbriteEvent {
    implicit val eventbriteEventDecoder: Decoder[EventbriteEvent] = new Decoder[EventbriteEvent] {
      override def apply(c: HCursor): Result[EventbriteEvent] = for {
        name <- c.downField("name").downField("text").as[String]
        description <- c.downField("description").downField("text").as[Option[String]]
        url <- c.downField("url").as[String]
        beginDateTimeUtc <- c.downField("start").downField("utc").as[String]
        endDateTimeUtc <- c.downField("end").downField("utc").as[Option[String]]
        status <- c.downField("status").as[String]
        onlineEvent <- c.downField("online_event").as[Boolean]
        isSeries <- c.downField("is_series").as[Boolean]
        free <- c.downField("is_free").as[Boolean]
        logoUrl <- c.downField("logo").downField("original").downField("url").as[String]
        organizer <- c.downField("organizer").as[Option[EventbriteOrganizer]]
        venue <- c.downField("venue").as[Option[EventbriteVenue]]
      } yield EventbriteEvent(
        name,
        description,
        url,
        beginDateTimeUtc,
        endDateTimeUtc,
        status,
        onlineEvent,
        isSeries,
        free,
        logoUrl,
        organizer,
        venue
      )
    }
  }

  final case class Pagination(objectCount: Option[Int],
                              pageNumber: Int,
                              pageSize: Int,
                              pageCount: Option[Int],
                              moreItems: Boolean)
  object Pagination {
    implicit val paginationDecoder: Decoder[Pagination] = new Decoder[Pagination]{
      override def apply(c: HCursor): Result[Pagination] = for {
        objectCount <- c.downField("object_count").as[Option[Int]]
        pageNumber <- c.downField("page_number").as[Int]
        pageSize <- c.downField("page_size").as[Int]
        pageCount <- c.downField("page_count").as[Option[Int]]
        moreItems <- c.downField("has_more_items").as[Boolean]
      } yield Pagination(objectCount, pageNumber, pageSize, pageCount, moreItems)
    }
  }

  trait EventbriteResponse

  final case class SuccessEbResponse(events: List[EventbriteEvent], pagination: Pagination) extends EventbriteResponse
  object SuccessEbResponse{
    implicit val successEbResponseDecoder: Decoder[SuccessEbResponse] = new Decoder[SuccessEbResponse] {
      override def apply(c: HCursor): Result[SuccessEbResponse] = for {
        pagination <- c.downField("pagination").as[Pagination]
        events <- c.downField("events").as[List[EventbriteEvent]]
      } yield SuccessEbResponse(events, pagination)
    }
  }
  final case class ParsingResponseFailure(failure: DecodeFailure) extends EventbriteResponse
  final case class FailedEbResponse(status: Status, details: String) extends EventbriteResponse

}