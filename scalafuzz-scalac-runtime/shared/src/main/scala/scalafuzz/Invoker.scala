package scalafuzz

import scalafuzz.Platform._

/** based on @author Stephen Samuel work*/
object Invoker {

  type DataDir = String
  type InvocationId = Int
  type InvocationCount = Int

  // For each data directory we maintain a thread-safe set tracking the ids that we've already
  // seen and recorded. We're using a map as a set, so we only care about its keys and can ignore
  // its values.
  private val dataDirToIds =
    ThreadSafeMap.empty[DataDir, ThreadSafeMap[InvocationId, InvocationCount]]

  /**
   * We record that the given id has been invoked.
   *
   * This will happen concurrently on as many threads as the application is using.
   *
   * @param id the id of the statement that was invoked
   * @param dataDir the directory where the measurement data is held
   */
  def invoked(id: InvocationId, dataDir: DataDir): Unit = {
    // [sam] we can do this simple check to save writing out to a file.
    // This won't work across JVMs but since there's no harm in writing out the same id multiple
    // times since for coverage we only care about 1 or more, (it just slows things down to
    // do it more than once), anything we can do to help is good. This helps especially with code
    // that is executed many times quickly, eg tight loops.
    if (!dataDirToIds.contains(dataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(dataDir)) {
          dataDirToIds(dataDir) = ThreadSafeMap.empty[InvocationId, InvocationCount]
        }
      }
    }
    dataDirToIds.synchronized {
      val ids = dataDirToIds(dataDir)
      if (!ids.contains(id)) {
        ids.put(id, 1)
      } else {
        ids.put(id, ids.get(id).get + 1)
      }
    }
  }

  def invocations(): ThreadSafeMap[DataDir, ThreadSafeMap[InvocationId, InvocationCount]] =
    dataDirToIds

  def reset(): Unit = dataDirToIds.clear()
}
