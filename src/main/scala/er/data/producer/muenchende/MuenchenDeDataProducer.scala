package er.data.producer.muenchende

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import er.data.producer.DataConverters._
import er.data.producer.eventbrite.EventbriteDataProducer.printStr
import er.data.producer.muenchende.MuenchenDe.MuenchenDeEventData
import er.data.producer.{ElasticSearch, Logger}
import fs2.Stream
import fs2.concurrent.Queue
import io.circe.syntax._

import scala.concurrent.ExecutionContext

object MuenchenDeDataProducer extends IOApp {

  implicit val blockingEC = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val convertF: MuenchenDeEventData => Option[CommonEventData] = muenchenDeToCommonEventData
  implicit val logger: Logger[IO] = Logger.ioLogger(this.getClass)
  implicit val encoder = CommonEventData.commonEventDataEncoder
  implicit val ioElasticSearch = ElasticSearch.ioElasticSearch

  def program: Stream[IO, Unit] = for {
    _ <- Stream.eval(printStr("Producing data from muenche.de"))
    q <- Stream.eval(Queue.bounded[IO, MuenchenDeEventData](100))
    buff = new MuenchenDeBuffering[IO](q, MuenchenDe.ioMuenchenDe.get, 71)
    _ <- Stream(
      buff.start,
      q.dequeue
        .mapAsyncUnordered(4)(ev => IO(convertF(ev)))
        .unNone
        .mapAsyncUnordered(4)(cd => ioElasticSearch.send(cd.asJson))
    ).parJoin(2)
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = program.compile.drain.attempt.flatMap {
    case Left(err) => logger.logError(s"muenche.de producer error. ${err.getMessage}", err)
      .map(_ => ExitCode.Error)
    case Right(_) => logger.logInfo("Program finished successfully.").map(_ => ExitCode.Success)
  }
}
