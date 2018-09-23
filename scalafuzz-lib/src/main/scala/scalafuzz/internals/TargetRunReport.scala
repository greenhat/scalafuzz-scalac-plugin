package scalafuzz.internals

import scalafuzz.Invoker.InvocationId

sealed trait TargetExitStatus
case object TargetNormalExit extends TargetExitStatus
case class TargetExceptionThrown(e: Throwable) extends TargetExitStatus

case class TargetRunReport(input: Array[Byte], exitStatus: TargetExitStatus, invocations: Seq[InvocationId])

