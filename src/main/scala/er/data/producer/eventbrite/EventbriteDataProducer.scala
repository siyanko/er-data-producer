package er.data.producer.eventbrite

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.Logger

import scala.concurrent.ExecutionContext

object EventbriteDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  val ioEventBrite = EventbriteService.ioEventbriteService
  val logger: Logger[IO] = Logger.ioLogger(this.getClass)

  def program: IO[Unit] = for {
    resp <- ioEventBrite.getMunichEvents("XXX")
    _ <- logger.logInfo(s"RESP: $resp")
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.attempt.flatMap {
    case Left(err) =>
      logger.logError(s"Eventbrite producer error. ${err.getMessage}", err)
        .map(_ => ExitCode.Error)
    case Right(_) => IO(ExitCode.Success)
  }
}
