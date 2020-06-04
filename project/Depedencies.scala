import sbt._

object Dependencies {

  val all = Seq(
    config.pureconfig,

    https.client,

    cats.core,
    cats.effect,
    tofu.core,
    zio.core,
    zio.`cats-interop`,
    tofu.logging,
    monix.core,

    doobie.core,
    doobie.hikari,
    clickhouse.driver,

    logback.classic,
    xml.core,
    scalaTest.core % Test
  )

  object xml {
    val version = "2.0.0-M1"

    val core = "org.scala-lang.modules" %% "scala-xml" % version
  }

  object config {
    val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.3"
  }

  object https {
    val version = "0.21.1"

    val client = "org.http4s" %% "http4s-blaze-client" % version
  }

  object tofu {
    val version = "0.7.3"

    val core = "ru.tinkoff" %% "tofu" % version
    val logging = "ru.tinkoff" %% "tofu-logging" % version
    val `logging-layout` = "ru.tinkoff" %% "tofu-logging-layout" % version
  }

  object cats {
    val effectVersion = "2.1.2"
    val coreVersion = "2.1.1"

    val core   = "org.typelevel" %% "cats-core" % coreVersion
    val effect = "org.typelevel" %% "cats-effect" % effectVersion
  }

  object monix {
    val version = "3.1.0"

    val core = "io.monix" %% "monix" % version
  }

  object zio {
    val core = "dev.zio" %% "zio" % "1.0.0-RC18-2"
    val `cats-interop` = "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC1"
  }

  object doobie {
    val version = "0.8.8"

    val core   = "org.tpolecat" %% "doobie-core" % version
    val hikari = "org.tpolecat" %% "doobie-hikari" % version
  }

  object clickhouse {
    val version = "0.2.4"

    val driver = "ru.yandex.clickhouse" % "clickhouse-jdbc" % version
  }

  object logback {
    val version = "1.2.3"

    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object scalaTest {
    val version = "3.1.1"

    val core = "org.scalatest" %% "scalatest" % version
  }
}
