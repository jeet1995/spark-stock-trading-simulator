name := "stock-trading-simulation"

version := "0.1"

scalaVersion := "2.11.8"

sbtVersion := "1.1.2"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.apache.spark" %% "spark-core" % "2.4.4",
  "org.apache.httpcomponents" % "httpclient" % "4.5",
  "org.apache.spark" %% "spark-hive" % "2.4.4",
  "org.apache.spark" %% "spark-sql" % "2.4.4"
)

mainClass in(Compile, run) := Some("com.trading.simulation.StockTradingJobDriver")
mainClass in assembly := Some("com.trading.simulation.StockTradingJobDriver")
