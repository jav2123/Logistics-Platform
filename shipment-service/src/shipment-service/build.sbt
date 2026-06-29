name := """shipment-service"""
organization := "jro.microservices"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "jro.microservices.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "jro.microservices.binders._"
