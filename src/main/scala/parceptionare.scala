import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URI
import java.util.concurrent.TimeUnit.SECONDS
import java.util.Arrays.asList
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.google.cloud.pubsub.client.{Message, Pubsub}
import com.typesafe.config.ConfigFactory
import models.ParseData
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import parsing.Parser
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSResponseHeaders
import play.api.libs.ws.ahc.AhcWSClient
import com.spotify.google.cloud.pubsub.client.Message.encode

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.control.Breaks._

object parceptionare extends App {
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val conf = ConfigFactory.load()
  val project = conf.getString("google.pubsub.project")
  val topic = conf.getString("google.pubsub.topic")
  val subscription = conf.getString("google.pubsub.subscription")

  val pubsub = Pubsub.builder()
  .uri(URI.create("http://localhost:8555/v1/"))
      .requestTimeout(10000000)
    .readTimeout(10000000)
  .build()

//  pubsub.createTopic(project, topic).get() // only for testing

    val future = pubsub.getSubscription(project, subscription)
    val result = future.get(10, SECONDS)
    if(result == null) {
      pubsub.createSubscription(project, subscription, topic).get()
    }

  val messages = asList(
    Message.builder()
      .data(encode(Json.toJson(ParseData("3162896504700534850", "CSGO-ecQfW-dDxvQ-3Z7bY-epQWB-uT2JE", "http://replay184.valve.net/730/003162902861252132998_0957369754.dem.bz2", 2845.toLong, 1472838458.toLong)).toString()))
      .build())
  pubsub.publish(project, topic, messages).get()

  while(true) {
    // return immediately and only take one message at a time
    for(message <- pubsub.pull(project, subscription, true, 1).get()) {
      try {
        Json.parse(message.message().decodedDataUTF8().toString).validate[ParseData] match {
          case s: JsSuccess[ParseData] =>
            val parseData: ParseData = s.get
            println(parseData)
            startParsing(parseData).map { parseIsSuccessful =>
              // always acknowledge because we don't want to parse failing demos sagain
              // just log this if it fails
              println("READY")
            }
          case e: JsError =>
            println(e)
        }
      } catch {
        case e: Exception => println(e)
      }
      pubsub.acknowledge(project, subscription, message.ackId()).get()
    }
    Thread.sleep(200000)
  }

  def startParsing(parseData: ParseData) = {
    downloadDemoFile(parseData).map { finishedFileOption =>
      finishedFileOption.exists { finishedFile =>
        try {
          val file = getBufferedReaderForCompressedFile(finishedFile)
          Parser.startParsing(parseData, file)
        } catch {
          case e: Exception => println(e.getMessage); false
        }
      }
    }.recover { case _ => println("recover"); false }
  }

  private def downloadDemoFile(parseData: ParseData) = {
    val wsClient = AhcWSClient()
    //at the very end, to shutdown stuff cleanly :
    val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
      wsClient.url(parseData.url).getStream()


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
            Future(None)
          case _ =>
            Logger.error("Demo file download failed")
            Future(None)
        }
    }
    //wsClient.close()
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
        out.write(buf, 0, read)
      }
    }
    new File(fileIn.getAbsolutePath).delete()
    tmpFile
  }
}
