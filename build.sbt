name := "akka-samples2"

version := "1.0"

scalaVersion := "2.13.12"

val akkaVersion = "2.6.21"

Compile / scalacOptions ++= Seq(
  "-Xsource:3",
  "-release:17",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Xlint",
  "-Wconf:cat=other-match-analysis:error", //Transform exhaustivity warnings into errors.
  "-Wconf:msg=lambda-parens:s",
  "-Xmigration" //Emit migration warnings under -Xsource:3 as fatal warnings, not errors; -Xmigration disables fatality (Demote the errors to warnings)
)

libraryDependencies ++= Seq(
  //"com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.7",

  "org.scala-lang"  %  "scala-reflect" % scalaVersion.value,
  
  "com.beachape" %% "enumeratum" % "1.7.2",

  "com.lihaoyi" % "ammonite" % "3.0.0-M0-52-d2acc162" % "test" cross CrossVersion.full
)

scalafmtOnCompile := true

addCommandAlias("c", "compile")
addCommandAlias("r", "reload")

//test:run
Test / sourceGenerators += Def.task {
  val file = (Test / sourceManaged).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue


//run / fork := false //true