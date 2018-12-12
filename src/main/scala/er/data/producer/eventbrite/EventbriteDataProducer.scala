package er.data.producer.eventbrite

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.Logger
import er.data.producer.eventbrite.EventbriteService._
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object EventbriteDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  val ioEventBrite = EventbriteService.ioEventbriteService
  val logger: Logger[IO] = Logger.ioLogger(this.getClass)

  val readInput: IO[String] = IO(StdIn.readLine())
  val printStr: String => IO[Unit] = s => IO(println(s))

  def program: IO[Unit] = for {
    _ <- printStr("Enter eventbrite access token")
    token <- readInput
    resp <- ioEventBrite.getMunichEvents(token)
    _ <- resp match {
      case SuccessEbResponse(ls, pagination) => ls.traverse[IO, Unit](ev => logger.logInfo(ev.name))
      case FailedEbResponse(status, details) => logger.logError(s"Eventbrite error. Status: $status. Details: $details")
      case ParsingResponseFailure(failure) => logger.logError(failure.getMessage(), failure)
    }
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.attempt.flatMap {
    case Left(err) =>
      logger.logError(s"Eventbrite producer error. ${err.getMessage}", err)
        .map(_ => ExitCode.Error)
    case Right(_) => IO(ExitCode.Success)
  }
}
