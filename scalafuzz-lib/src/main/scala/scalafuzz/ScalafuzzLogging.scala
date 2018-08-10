package scalafuzz

import com.typesafe.scalalogging.StrictLogging

trait ScalafuzzLogging extends StrictLogging {
  @inline protected def log = logger
}
