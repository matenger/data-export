name := "data-export"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= {
  val akkaV = "2.5.13"
  val postgresV = "42.2.2"
  val hikariV = "3.2.0"
  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "org.postgresql" % "postgresql" % postgresV,
    "com.zaxxer" % "HikariCP" % hikariV
  )
}
