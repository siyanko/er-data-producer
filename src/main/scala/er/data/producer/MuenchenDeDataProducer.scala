package er.data.producer

import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.DataConverters.CommonEventData._
import er.data.producer.DataConverters._
import er.data.producer.MuenchenDe._
import io.circe.Encoder
import io.circe.syntax._

object MuenchenDeDataProducer extends IOApp {

  implicit val convertF: MuenchenDeEventData => IO[CommonEventData] = muenchenDeToCommonEventData(Applicative[IO])
  implicit val logger: Logger[IO] = Logger.ioLogger(this.getClass)

  def program(implicit mde: MuenchenDe[IO],
              es: ElasticSearch[IO],
              convertF: MuenchenDeEventData => IO[CommonEventData],
              logger: Logger[IO],
              encoder: Encoder[CommonEventData]): IO[Unit] = for {
    _ <- logger.logInfo("Producing data from muenche.de")
    rawData <- mde.get("http://test.url")
    _ <- logger.logInfo("Converting raw data")
    commonData <- convertF(rawData)
    _ <- logger.logInfo("Sending data to elastic search")
    _ <- es.send(commonData.asJson)
    _ <- logger.logInfo("Finished Job.")
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.attempt.flatMap {
    case Left(err) => Logger[IO].logError(err.getMessage, err).map(_ => ExitCode.Error)
    case Right(_) => IO(ExitCode.Success)
  }
}
