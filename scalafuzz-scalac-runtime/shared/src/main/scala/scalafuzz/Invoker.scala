package scalafuzz

import java.util.concurrent.ConcurrentLinkedQueue

import scalafuzz.Platform._

/** based on @author Stephen Samuel work*/
object Invoker {

  type DataDir = String
  type InvocationId = Int

  type ThreadSafeQueue[A] = ConcurrentLinkedQueue[A]

  private val dataDirToIds =
    ThreadSafeMap.empty[DataDir, ThreadSafeQueue[InvocationId]]

  @inline
  private def invocationQueue(dataDir: DataDir): ThreadSafeQueue[InvocationId] = {
    if (!dataDirToIds.contains(dataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(dataDir)) {
          dataDirToIds(dataDir) = new ThreadSafeQueue[InvocationId]()
        }
      }
    }
    dataDirToIds(dataDir)
  }

  /**
    * We record that the given id has been invoked.
    *
    * This will happen concurrently on as many threads as the application is using.
    *
    * @param id the id of the statement that was invoked
    * @param dataDir the directory where the measurement data is held
   */
  def invoked(id: InvocationId, dataDir: DataDir): Unit = {
    invocationQueue(dataDir).add(id)
  }

  def invocations(): ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]=
    dataDirToIds

  def reset(): Unit = {
    dataDirToIds.clear()
  }
}
