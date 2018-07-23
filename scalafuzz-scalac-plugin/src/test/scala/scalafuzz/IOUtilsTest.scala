package scalafuzz

import java.io.{File, FileWriter}
import java.util.UUID

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, Matchers, OneInstancePerTest}

/** @author Stephen Samuel */
class IOUtilsTest extends FreeSpec with MockitoSugar with OneInstancePerTest with Matchers {

  "io utils" - {
    "should deep search for report files" in {
      // create new folder to hold all our data
      val base = new File(IOUtils.getTempDirectory, UUID.randomUUID.toString)
      base.mkdir() shouldBe true

      val file1 = new File(base + "/" + Constants.XMLReportFilename)
      val writer1 = new FileWriter(file1)
      writer1.write("1\n3\n5\n\n\n7\n")
      writer1.close()

      val file2 = new File(base + "/" + UUID.randomUUID + "/" + Constants.XMLReportFilename)
      file2.getParentFile.mkdir()
      val writer2 = new FileWriter(file2)
      writer2.write("2\n4\n6\n\n8\n")
      writer2.close()

      val file3 = new File(file2.getParent + "/" + UUID.randomUUID + "/" + Constants.XMLReportFilename)
      file3.getParentFile.mkdir()
      val writer3 = new FileWriter(file3)
      writer3.write("11\n20\n30\n\n44\n")
      writer3.close()

      val files = IOUtils.reportFileSearch(base, IOUtils.isReportFile)
      val invoked = IOUtils.invoked(files)
      assert(invoked.toSet === Set(1, 2, 3, 4, 5, 6, 7, 8, 11, 20, 30, 44))

      file1.delete()
      file2.delete()
    }
  }
}
