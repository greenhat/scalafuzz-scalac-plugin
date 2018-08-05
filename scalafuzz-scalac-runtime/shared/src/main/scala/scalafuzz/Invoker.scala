package scalafuzz

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import scalafuzz.Platform._

/** based on @author Stephen Samuel work*/
object Invoker {

  type DataDir = String
  type InvocationId = Int
  type ThreadId = Long
  type ThreadIdIndex = Short

  type ThreadSafeQueue[A] = ConcurrentLinkedQueue[A]

  case class Invocation(threadIdIndex: ThreadIdIndex, invocationId: InvocationId)

  private val dataDirToIds =
    ThreadSafeMap.empty[DataDir, ThreadSafeQueue[Invocation]]

  // to store 2 bytes per invocation instead of 8
  private val threadIndices = ThreadSafeMap.empty[ThreadId, ThreadIdIndex]

  private var freeThreadIndex = new AtomicInteger()

  @inline
  private def threadIndex(threadId: ThreadId): ThreadIdIndex = {
    if (!threadIndices.contains(threadId)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      threadIndices.synchronized {
        if (!threadIndices.contains(threadId)) {
          threadIndices(threadId) = freeThreadIndex.getAndIncrement().toShort
        }
      }
    }
    threadIndices(threadId)
  }

  @inline
  private def invocationQueue(dataDir: DataDir): ThreadSafeQueue[Invocation] = {
    if (!dataDirToIds.contains(dataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(dataDir)) {
          dataDirToIds(dataDir) = new ThreadSafeQueue[Invocation]()
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
    invocationQueue(dataDir).add(Invocation(threadIndex(Thread.currentThread.getId), id))
  }

  def invocations(): ThreadSafeMap[DataDir, ThreadSafeQueue[Invocation]]=
    dataDirToIds

  def reset(): Unit = {
    freeThreadIndex = new AtomicInteger()
    threadIndices.clear()
    dataDirToIds.clear()
  }
}
