package actors

import java.io.{FileInputStream, FileOutputStream, File}

import org.joda.time.DateTime

import scala.util.{Success, Failure}
import akka.actor.{Props, Actor}
import models.ParseData
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import parsing.Parser
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.ws.{WS, WSResponseHeaders}
import services.{ShareCodeDao, ParseQueueDao}
import scala.concurrent.Future
import scala.util.control.Breaks._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

class ShareCodeQueueActor extends Actor {
  lazy val parseQueueDao = current.injector.instanceOf[ParseQueueDao]
  var currentParsingQueue = 0
  var parsingQueueTimings = List[(DateTime, String)]()
  lazy val shareCodeDao = current.injector.instanceOf[ShareCodeDao]

  private def downloadDemoFile(parseData: ParseData) = {
    val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
      WS.url(parseData.url).getStream()

    val downloadedFile: Future[Option[File]] = futureResponse.flatMap {
      case (headers, body) =>
        headers.status match {
          case 200 =>
            val tmpFileName = parseData.matchId
            val tmpFile = new TemporaryFile(File.createTempFile(tmpFileName, ".dem.bz2"))
            val outputStream = new FileOutputStream(tmpFile.file)

            val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
              outputStream.write(bytes)
            }
            (body |>>> iteratee).andThen {
              case result =>
                outputStream.close()
                result.get
            }.map(_ => Some(tmpFile.file))
          case 404 =>
            Logger.error("Demo file with url %s download from valve servers failed".format(parseData.url))
            shareCodeDao.updateShareCodeStatus(parseData.shareCode, "expired")
            Future(None)
          case _ =>
            Logger.error("Demo file download failed")
            shareCodeDao.updateShareCodeStatus(parseData.shareCode, "error")
            Future(None)
        }
    }
    downloadedFile
  }

  def getBufferedReaderForCompressedFile(fileIn: File): TemporaryFile = {
    val tmpFileName = fileIn.getName.replace(".bz2", "").replace(".dem", "")
    val tmpFile = new TemporaryFile(File.createTempFile(tmpFileName, ".dem"))
    val in = new FileInputStream(fileIn.getAbsolutePath)
    val out = new FileOutputStream(tmpFile.file.getAbsolutePath)
    val bzIn = new BZip2CompressorInputStream(in)
    val buf = new Array[Byte](2048)
    breakable {
      while (true) {
        val read = bzIn.read(buf)
        if (read == -1) {
          out.close()
          bzIn.close()
          in.close()
          break
        }
        out.write(buf, 0, read);
      }
    }
    new File(fileIn.getAbsolutePath).delete()
    tmpFile
  }

  def receive = {
    case CheckQueue => {
      println("parsing count: " + currentParsingQueue)
      if (currentParsingQueue < 1) {
        val future = parseQueueDao.queryQueue
        future.onComplete {
          case Success(result) => {
            result.headOption.map { parseQueue =>
              currentParsingQueue = currentParsingQueue + 1
              println("start parse")
              parsingQueueTimings = (new DateTime(), parseQueue._2) :: parsingQueueTimings
              val parseData = ParseData(parseQueue._1, parseQueue._2, parseQueue._3, parseQueue._4, parseQueue._5)
              downloadDemoFile(parseData).map { finishedFileOption =>
                finishedFileOption.map { finishedFile =>
                  try {
                    val file = getBufferedReaderForCompressedFile(finishedFile)
//                    val parser = Akka.system.actorOf(Props[ParserActor])
//                    parser !(self, parseData, file)
                  } catch {
                    case e: Exception =>  self ! DecrementParsingCount(parseQueue._2); println(e.getMessage)
                  }
                }.getOrElse {
                  parseQueueDao.deleteByShareCode(parseQueue._2)
                  shareCodeDao.updateShareCodeStatus(parseData.shareCode, "expired")
                  println("demo has expired")
                  self ! DecrementParsingCount(parseQueue._2)
                }
              }.recover { case _ => println("recover"); self ! DecrementParsingCount(parseQueue._2) }
            }
          }
          case Failure(t) => println("Nothing to parse: " + t.getMessage)
        }
      } else {
        parsingQueueTimings.foreach { queueItem => {
          println("check timings")
          if (queueItem._1.isBefore(new DateTime().minusMinutes(5))) {
            println("timeout demo")
            self ! DecrementParsingCount(queueItem._2)
            shareCodeDao.updateShareCodeStatus(queueItem._2, "timeout")
            parseQueueDao.deleteByShareCode(queueItem._2)
          }
        }
        }
      }
    }
    case DecrementParsingCount(shareCode: String) => {
      currentParsingQueue = currentParsingQueue - 1
      parsingQueueTimings = parsingQueueTimings.filterNot(queueItem => queueItem._2 == shareCode)
    }
  }
}

case class CheckQueue()

case class DecrementParsingCount(shareCode: String)

//
//val pubsub = Pubsub.builder()
//.uri(URI.create("http://localhost:8143/v1/"))
//.build()
//
//// pubsub.createTopic("test-project", "topic1").get()
//
//// pubsub.createSubscription("test-project", "subscription-name", "topic1").get()
//
//val messages = asList(
//Message.builder()
//.attributes("type", "foo")
//.data(encode("hello foo"))
//.build(),
//Message.builder()
//.attributes("type", "bar")
//.data(encode("hello foo"))
//.build());
//
//val messageIds = pubsub.publish("test-project", "topic1", messages).get()
//
//val received = pubsub.pull("test-project", "subscription-name").get()
//System.out.println("Received Messages: " + received)
//
//val ackIds = received.map(id => id.ackId().toString).toList
//pubsub.acknowledge("test-project", "subscription-name", ackIds).get()