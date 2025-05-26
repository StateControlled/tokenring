package application.client

import akka.actor.{Actor, Address}
import com.typesafe.config.ConfigFactory
import application.server.Token

class Client extends Actor {
    private val config = ConfigFactory.load()
    private val remoteAddress: Address = setRemoteAddress()

    override def receive: Receive = {
        case token: Token =>
            println(s"Received token ${token.id}")
    }

    private def setRemoteAddress(): Address = {
        val hostname = config.getString("server.akka.remote.artery.canonical.hostname")
        val port = config.getInt("server.akka.remote.artery.canonical.port")

        val remoteAddress: Address = Address("akka", "remote_system", hostname, port)
        println("Remote address set to " + remoteAddress.toString)
        // return
        remoteAddress
    }

}
