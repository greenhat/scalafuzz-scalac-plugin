package scalafuzz

import cats.effect.{IO, Sync}
import org.slf4j.LoggerFactory

trait Log[F[_]] {
  def info(s: String): F[Unit]
  def error(e: Throwable): F[Unit]
}
object Log {
  def apply[F[_]](implicit ev: Log[F]): Log[F] = ev
  object io extends SyncLog[IO]
}

class SyncLog[F[_]](implicit F: Sync[F]) extends Log[F] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def info(s: String): F[Unit] = F.delay(logger.info(s))
  override def error(e: Throwable): F[Unit] = F.delay(logger.error(e.getMessage, e))
}

