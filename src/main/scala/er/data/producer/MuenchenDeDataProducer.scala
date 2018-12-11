package er.data.producer

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import er.data.producer.DataConverters._
import er.data.producer.MuenchenDe._
import io.circe.Encoder
import io.circe.syntax._

import scala.concurrent.ExecutionContext

object MuenchenDeDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val convertF: MuenchenDeEventData => Option[CommonEventData] = muenchenDeToCommonEventData
  implicit val logger: Logger[IO] = Logger.ioLogger(this.getClass)
  implicit val encoder = CommonEventData.commonEventDataEncoder
  implicit val ioElasticSearch = ElasticSearch.ioElasticSearch

  def program(implicit mde: MuenchenDe[IO],
              es: ElasticSearch[IO],
              convertF: MuenchenDeEventData => Option[CommonEventData],
              logger: Logger[IO],
              encoder: Encoder[CommonEventData]): IO[Unit] = for {
    _ <- logger.logInfo("Producing data from muenche.de")
    muenchenDeEvents <- mde.get(100)
    _ <- logger.logInfo("Sending data to elastic search")
    _ <- muenchenDeEvents
      .map(convertF)
      .flatten
      .traverse[IO, Unit](data => es.send(data.asJson))
    _ <- logger.logInfo("Finished Job.")
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.attempt.flatMap {
    case Left(err) => Logger[IO].logError(err.getMessage, err).map(_ => ExitCode.Error)
    case Right(_) => IO(ExitCode.Success)
  }
}
