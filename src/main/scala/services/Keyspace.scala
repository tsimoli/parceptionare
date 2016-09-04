package services

import com.websudos.phantom.connectors.{ContactPoints, KeySpace}

import com.datastax.driver.core.Session
import com.websudos.phantom.connectors._

trait BasicConnector extends DefaultConnector.connector.Connector

trait KeyspaceDefinition {
  implicit val space = KeySpace("parception")
}

object DefaultConnector extends KeyspaceDefinition {

  val connector = ContactPoint.local.keySpace(space.name)
}

object RemoteConnector extends KeyspaceDefinition {

  // Simply specify the list of hosts followed by the keyspace.
  // Now the connector object will automatically create the Database connection for us and initialise it.
  val connector = ContactPoints(Seq("docker.local")).keySpace("phantom_example")
}

