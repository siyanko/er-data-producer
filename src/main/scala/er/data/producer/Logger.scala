package er.data.producer

import cats.effect.IO
import org.slf4j.LoggerFactory

trait Logger[F[_]] {

  def logInfo(message: String): F[Unit]

  def logError(message: String): F[Unit]

  def logError(message: String, throwable: Throwable): F[Unit]

}

object Logger {
  def apply[F[_]](implicit F: Logger[F]): Logger[F] = F

  implicit def ioLogger(clazz: Class[_]): Logger[IO] = new Logger[IO] {

    val slf4JLogger = LoggerFactory.getLogger(clazz)

    override def logInfo(message: String): IO[Unit] = IO(slf4JLogger.info(message))

    override def logError(message: String): IO[Unit] =  IO(slf4JLogger.error(message))

    override def logError(message: String, throwable: Throwable):  IO[Unit] = IO(slf4JLogger.error(message, throwable))
  }
}
