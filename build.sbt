val akkaVersion = "2.8.8"

lazy val commonSettings = Seq(
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.3.6",
    resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    libraryDependencies += "org.scala-lang" %% "toolkit" % "0.7.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion

)

lazy val root = (project in file("."))
    .settings(commonSettings *)
    .settings(
        name := "tokenring"
    )
