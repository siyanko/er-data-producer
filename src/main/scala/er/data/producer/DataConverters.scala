package er.data.producer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import er.data.producer.MuenchenDe._
import io.circe.{Encoder, Json}
import io.circe.syntax._


import scala.util.Try

/*
  Contains converters from different event pages to common event data
 */
object DataConverters {


  val toLocalDateTime: String => Option[LocalDateTime] = timeStamp =>
    Try(DateTimeFormatter.ISO_DATE_TIME.parse(timeStamp)).map(LocalDateTime.from).toOption

  val toAddress: MuenchenDeAddress => Address = origin => Address(origin.streetAddress, origin.postalCode, origin.addressLocality)

  val toDouble: String => Option[Double] = s => Try(s.toDouble).toOption
  val toGeoLocation: MuenchenDeGeoLocation => Option[GeoLocation] = origin => for {
    lat <- toDouble(origin.latitude)
    lon <- toDouble(origin.longitude)
  } yield GeoLocation(lat, lon)


  val muenchenDeToCommonEventData: MuenchenDeEventData => Option[CommonEventData] = data => for {
    beginDateTimeStr <- data.terminIso
    beginDateTime <- toLocalDateTime(beginDateTimeStr + ":00")
    originUrl <- data.detailLink
  } yield CommonEventData(
    data.etName,
    beginDateTime,
    data.endterminIso.flatMap(d => toLocalDateTime(d + ":00")),
    data.extUrl,
    data.detailLogoUrl,
    originUrl,
    data.location.flatMap(_.address).map(toAddress),
    data.location.flatMap(_.geo).flatMap(toGeoLocation),
    "DE",
    "Munich"
  )

  final case class Address(street: String, zip: String, locality: String)

  final case class GeoLocation(lat: Double, lon: Double)

  final case class CommonEventData(eventName: String,
                                   beginDateUtc: LocalDateTime,
                                   endDateUtc: Option[LocalDateTime],
                                   ticketsUrl: Option[String],
                                   logoUrl: Option[String],
                                   originUrl: String,
                                   address: Option[Address],
                                   geoLocation: Option[GeoLocation],
                                   country: String,
                                   locality: String
                                  )

  object CommonEventData {

    implicit val geoLocationEncoder: Encoder[GeoLocation] = new Encoder[GeoLocation] {
      override def apply(a: GeoLocation): Json = Json.obj(
        "lat" -> Json.fromDoubleOrNull(a.lat),
        "lon" -> Json.fromDoubleOrNull(a.lon)
      )
    }

    implicit val addressEncode: Encoder[Address] = new Encoder[Address] {
      override def apply(a: Address): Json = Json.obj(
        "street" -> Json.fromString(a.street),
        "zip" -> Json.fromString(a.zip),
        "locality" -> Json.fromString(a.locality)
      )
    }

    val commonEventDataEncoder: Encoder[CommonEventData] = new Encoder[CommonEventData] {
      override def apply(a: CommonEventData): Json = {
        val data: Seq[(String, Json)] = List(
          Some("eventName" -> Json.fromString(a.eventName)),
          Some("beginDateUtc" -> Json.fromString(DateTimeFormatter.ISO_DATE_TIME.format(a.beginDateUtc))),
          Some("originUrl" -> Json.fromString(a.originUrl)),
          Some("country" -> Json.fromString(a.country)),
          Some("locality" -> Json.fromString(a.locality)),
          a.endDateUtc.map(d => "endDateUtc" -> Json.fromString(DateTimeFormatter.ISO_DATE_TIME.format(d))),
          a.ticketsUrl.map(d => "ticketsUrl" -> Json.fromString(d)),
          a.logoUrl.map(d => "logoUrl" -> Json.fromString(d)),
          a.address.map(d => "address" -> d.asJson),
          a.geoLocation.map(d => "geoLocation" -> d.asJson)
        ).flatten

        Json.fromFields(data)
      }
    }
  }

}
