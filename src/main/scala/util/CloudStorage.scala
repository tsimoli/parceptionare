package util

import java.io.{File, FileInputStream}
import java.util

import com.google.api.client.googleapis.compute.ComputeCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.model.{ObjectAccessControl, StorageObject}

object CloudStorage {
  val BUCKET_NAME = "playin-bucket"

  val JSON_FACTORY = JacksonFactory.getDefaultInstance
  val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
  val credential = new ComputeCredential.Builder(httpTransport, JSON_FACTORY).build()

  // Set up global Storage instance.
  val client = new Storage.Builder(httpTransport, JSON_FACTORY, credential)
    .setApplicationName("playin-123").build()

  def uploadFile(file: File, fileName: String) = {
    try {
      val mediaContent = new InputStreamContent("application/octet-stream", new FileInputStream(file))
      // Set the access control list to publicly read-only
      val objectMetaData = new StorageObject().setName(fileName)
        .setAcl(util.Arrays.asList(new ObjectAccessControl().setEntity("allUsers").setRole("READER")))

      val insertObject = client.objects().insert(BUCKET_NAME, objectMetaData, mediaContent)
      insertObject.setName(fileName)
      insertObject.execute()
    } catch {
      case e: Exception => println("File upload failed: " + e.getMessage)
    }
  }
}