package er.data.producer

import MuenchenDe._
import cats.Applicative
import io.circe.{Encoder, Json}

/*
  Contains converters from different event pages to common event data
 */
object DataConverters {

  def muenchenDeToCommonEventData[F[_] : Applicative]: MuenchenDeEventData => F[CommonEventData] = data =>
    Applicative[F].pure(CommonEventData(data.etName))

  final case class CommonEventData(eventName: String)

  object CommonEventData {
    implicit val encoder: Encoder[CommonEventData] = new Encoder[CommonEventData] {
      override def apply(a: CommonEventData): Json = Json.obj(
        "eventName" -> Json.fromString(a.eventName)
      )
    }
  }

}
