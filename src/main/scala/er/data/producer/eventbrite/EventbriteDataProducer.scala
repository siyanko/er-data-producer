package er.data.producer.eventbrite

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.DataConverters._
import er.data.producer.Logger
import er.data.producer.eventbrite.EventbriteService._
import fs2.concurrent.Queue
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object EventbriteDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val encoder = CommonEventData.commonEventDataEncoder

  val ioEventBrite = EventbriteService.ioEventbriteService
  implicit val logger: Logger[IO] = Logger.ioLogger(this.getClass)

  val readInput: IO[String] = IO(StdIn.readLine())
  val printStr: String => IO[Unit] = s => IO(println(s))

  def program: IO[Unit] = for {
    _ <- printStr("Enter eventbrite access token")
    token <- readInput
    q <- Queue.bounded[IO, EventbriteEvent](100)
    buff = new EventbriteBuffering[IO](q, ioEventBrite.getMunichEvents(token))
    _ <- buff.start
    _ <- q.dequeue
      .mapAsyncUnordered(4)(ev => IO(eventbriteEventToCommonEventData(ev)))
      .unNone
      .mapAsyncUnordered(4)(cd => logger.logInfo(cd.asJson.toString))
      .compile.drain
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.attempt.flatMap {
    case Left(err) =>
      logger.logError(s"Eventbrite producer error. ${err.getMessage}", err)
        .map(_ => ExitCode.Error)
    case Right(_) => IO(ExitCode.Success)
  }
}
