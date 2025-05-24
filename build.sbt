lazy val commonSettings = Seq(
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.3.6",
    resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.8.8",
    libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.8.8"
)

lazy val root = (project in file("."))
    .settings(commonSettings *)
    .settings(
        name := "tokenring"
    )
