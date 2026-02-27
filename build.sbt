import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / scalaVersion := "3.8.2"
ThisBuild / organization := "zbooks"
ThisBuild / version      := "0.1.0-SNAPSHOT"

// zio-http pulls in zio-schema-json which depends on zio-json 0.7.x;
// we use 0.9.0 everywhere — allow the higher version to win without error.
ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % VersionScheme.Always

// ─── Shared (JVM + JS) ──────────────────────────────────────────────────────

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/shared"))
  .settings(
    name := "shared",
    libraryDependencies += "dev.zio" %%% "zio-json" % "0.9.0",
  )

// ─── Backend ─────────────────────────────────────────────────────────────────

lazy val backend = project
  .in(file("modules/backend"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(shared.jvm)
  .settings(
    name := "backend",
    libraryDependencies ++= Seq(
      "dev.zio"         %% "zio"       % "2.1.24",
      "dev.zio"         %% "zio-http"  % "3.8.1",
      "dev.zio"         %% "zio-json"  % "0.9.0",
      "com.augustnagro" %% "magnum"    % "1.3.1",
      "com.zaxxer"       % "HikariCP"  % "7.0.2",
      "com.h2database"   % "h2"        % "2.4.240",
    ),
    // Ensure resources are on the classpath
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources",
  )

// ─── Frontend ────────────────────────────────────────────────────────────────

lazy val frontend = project
  .in(file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(shared.js)
  .settings(
    name                            := "frontend",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo"    %%% "laminar"     % "17.2.1",
      "org.scala-js" %%% "scalajs-dom" % "2.8.1",
      "dev.zio"      %%% "zio-json"    % "0.9.0",
    ),
  )

// ─── Root ─────────────────────────────────────────────────────────────────────

lazy val root = project
  .in(file("."))
  .aggregate(shared.jvm, shared.js, backend, frontend)
  .settings(
    name           := "zbooks",
    publish / skip := true,
  )
