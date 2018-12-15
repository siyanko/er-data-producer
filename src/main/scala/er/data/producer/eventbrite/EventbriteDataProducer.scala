package er.data.producer.eventbrite

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.DataConverters._
import er.data.producer.{ElasticSearch, Logger}
import er.data.producer.eventbrite.EventbriteService._
import fs2.concurrent.Queue
import io.circe.syntax._
import fs2.Stream

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object EventbriteDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val encoder = CommonEventData.commonEventDataEncoder

  val ioEventBrite = EventbriteService.ioEventbriteService
  implicit val logger: Logger[IO] = Logger.ioLogger(this.getClass)
  implicit val ioElasticSearch = ElasticSearch.ioElasticSearch


  val readInput: IO[String] = IO(StdIn.readLine())
  val printStr: String => IO[Unit] = s => IO(println(s))

  def program: Stream[IO, Unit] = for {
    _ <- Stream.eval(printStr("Enter eventbrite access token"))
    token <- Stream.eval(readInput)
    q <- Stream.eval(Queue.bounded[IO, EventbriteEvent](100))
    buff = new EventbriteBuffering[IO](q, ioEventBrite.getMunichEvents(token))
    _ <- Stream(
      buff.start,
      q.dequeue
        .mapAsyncUnordered(4)(ev => IO(eventbriteEventToCommonEventData(ev)))
        .unNone
        .mapAsyncUnordered(4)(cd => ioElasticSearch.send(cd.asJson))
    ).parJoin(2)
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.compile.drain.attempt.flatMap {
    case Left(err) =>
      logger.logError(s"Eventbrite producer error. ${err.getMessage}", err)
        .map(_ => ExitCode.Error)
    case Right(_) => logger.logInfo("Program finished successfully.").map(_ => ExitCode.Success)
  }
}
