package application.util

import application.core.*

import scala.io.Source

object CustomFileReader extends App {
    readData()

    private def readData(): Unit = {
        // reads from src/main/resources
        val resource = Source.fromResource("events.json")

        // file contents to string
        val builder: StringBuilder = new StringBuilder()
        resource.getLines().foreach(line => builder.append(line))
        val jsonString: String = builder.toString()

        // deserialize json string
        val list: List[Event] = upickle.default.read[List[Event]](jsonString)
        list.foreach(event => println(event))
    }
}
