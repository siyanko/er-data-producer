package er.data.producer.eventbrite

import er.data.producer.eventbrite.EventbriteService.EventbriteEvent
import io.circe._
import io.circe.parser._
import org.scalatest.{EitherValues, Matchers, WordSpec}

class EventbriteEventSpec extends WordSpec with Matchers with EitherValues {

  "EventbriteEvent decoder" should {
    "decode event json" in {
      val eventJson: String = """
                                |{
                                | "name": {
                                |   "text": "moove your asana",
                                |   "html": "moove your asana"
                                | },
                                | "description": {
                                |   "text": "Welcome to this wonderful yoga flow full of flexibility work and fun, funky flows! We will work on creating space in the body to take us into deeper stretches and increasing our strength to support that flexibiliy. The class will be great for beginners, with plenty of alignment and instruction, but there will also be more challenging options for those who are interested.\nIt will be a pay what you want system, so there's no fixed price-you can pay on the day of!\nCan't wait to see yall there! The studio is located on the 5th floor.",
                                |   "html": "<P>Welcome to this wonderful yoga flow full of flexibility work and fun, funky flows!\u00a0 We will work on creating space in the body to take us into deeper stretches and increasing our strength to support that flexibiliy.\u00a0\u00a0The class will be great for beginners, with plenty of alignment and instruction, but there will also be more challenging options for those who are interested.\u00a0\u00a0<\/P>\n<P><SPAN>It will be a pay what you want system, so there's no fixed price-you can pay on the day of!<\/SPAN><\/P>\n<P><SPAN>Can't wait to see yall there! \u00a0The studio is located on the 5th floor.<\/SPAN><\/P>"
                                | },
                                | "id": "52748154149",
                                | "url": "https://www.eventbrite.com/e/moove-your-asana-tickets-52748154149",
                                | "start": {
                                |   "timezone": "Europe/Berlin",
                                |   "local": "2018-12-16T00:45:00",
                                |   "utc": "2018-12-15T23:45:00Z"
                                | },
                                | "end": {
                                |   "timezone": "Europe/Berlin",
                                |   "local": "2018-12-16T13:45:00",
                                |   "utc": "2018-12-16T12:45:00Z"
                                | },
                                | "organization_id": "263905033780",
                                | "created": "2018-11-17T09:58:30Z",
                                | "changed": "2018-12-16T01:25:32Z",
                                | "capacity": null,
                                | "capacity_is_custom": null,
                                | "status": "started",
                                | "currency": "EUR",
                                | "listed": true,
                                | "shareable": true,
                                | "online_event": false,
                                | "tx_time_limit": 480,
                                | "hide_start_date": false,
                                | "hide_end_date": false,
                                | "locale": "en_US",
                                | "is_locked": false,
                                | "privacy_setting": "unlocked",
                                | "is_series": true,
                                | "is_series_parent": false,
                                | "is_reserved_seating": false,
                                | "show_pick_a_seat": false,
                                | "show_seatmap_thumbnail": false,
                                | "show_colors_in_seatmap_thumbnail": false,
                                | "source": "create_2.0",
                                | "is_free": true,
                                | "version": "3.0.0",
                                | "logo_id": "49353361",
                                | "organizer_id": "17579457799",
                                | "venue_id": "27977531",
                                | "category_id": "107",
                                | "subcategory_id": "7005",
                                | "format_id": "9",
                                | "resource_uri": "https://www.eventbriteapi.com/v3/events/52748154149/",
                                | "is_externally_ticketed": false,
                                | "series_id": "49893600101",
                                | "logo": {
                                |   "crop_mask": {
                                |     "top_left": {"x": 0, "y": 258},
                                |     "width": 3096,
                                |     "height": 1548
                                |   },
                                |   "original": {
                                |     "url": "https://img.evbuc.com/https%3A%2F%2Fcdn.evbuc.com%2Fimages%2F49353361%2F263905033780%2F1%2Foriginal.jpg?auto=compress&s=db94e79f7f6020fe7f0cd7edc34e5aa4",
                                |     "width": 3097,
                                |     "height": 2065
                                |   },
                                |   "id": "49353361",
                                |   "url": "https://img.evbuc.com/https%3A%2F%2Fcdn.evbuc.com%2Fimages%2F49353361%2F263905033780%2F1%2Foriginal.jpg?h=200&w=450&auto=compress&rect=0%2C258%2C3096%2C1548&s=99e9d9d6ac24dd888c21b9ad394662e7",
                                |   "aspect_ratio": "2",
                                |   "edge_color": "#020606",
                                |   "edge_color_set": true
                                | }
                                |}
                              """.stripMargin

      val expected = EventbriteEvent(
        "moove your asana",
        Some("Welcome to this wonderful yoga flow full of flexibility work and fun, funky flows! We will work on creating space in the body to take us into deeper stretches and increasing our strength to support that flexibiliy. The class will be great for beginners, with plenty of alignment and instruction, but there will also be more challenging options for those who are interested.\nIt will be a pay what you want system, so there's no fixed price-you can pay on the day of!\nCan't wait to see yall there! The studio is located on the 5th floor."),
        "https://www.eventbrite.com/e/moove-your-asana-tickets-52748154149",
        "2018-12-16T00:45:00",
        Some("2018-12-16T13:45:00"),
        "started",
        false,
        true,
        true,
        Some("https://img.evbuc.com/https%3A%2F%2Fcdn.evbuc.com%2Fimages%2F49353361%2F263905033780%2F1%2Foriginal.jpg?auto=compress&s=db94e79f7f6020fe7f0cd7edc34e5aa4"),
        None,
        None
      )


      implicit val decoder: Decoder[EventbriteEvent] = EventbriteEvent.eventbriteEventDecoder

      val result = decode[EventbriteEvent](eventJson).right.value

      result.name shouldBe expected.name
      result.description shouldBe expected.description
      result.url shouldBe expected.url
      result.beginDateTimeUtc shouldBe expected.beginDateTimeUtc
      result.endDateTimeUtc shouldBe expected.endDateTimeUtc
      result.status shouldBe expected.status
      result.onlineEvent shouldBe expected.onlineEvent
      result.isSeries shouldBe expected.isSeries
      result.free shouldBe expected.free
      result.logoUrl shouldBe expected.logoUrl
      result.organizer shouldBe expected.organizer
      result.venue shouldBe expected.venue
    }
  }

}