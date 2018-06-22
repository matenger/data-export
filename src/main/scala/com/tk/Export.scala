package com.tk

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariDataSource

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Export extends App {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private val config: Config = ConfigFactory.load().getConfig("db")
  val dataSource = {
    val ds = new HikariDataSource()
    ds.setDataSourceClassName(config.getString("dataSourceClass"))
    ds.setUsername(config.getString("user"))
    ds.setPassword(config.getString("password"))
    ds.addDataSourceProperty("serverName", config.getString("serverName"))
    ds.addDataSourceProperty("portNumber", config.getString("portNumber"))
    ds.addDataSourceProperty("databaseName", config.getString("databaseName"))
    ds.setMaximumPoolSize(config.getInt("poolSize"))
    ds
  }

  case class Product(id: Long, name: String)

  val startProductId = 0
  val batchSize = 1000
  Source.unfold[Long, List[Product]](startProductId) { startId =>

    val batchResult = getProducts(startId, batchSize)

    if (batchResult.isEmpty) {
      None
    } else {
      Some(batchResult.last.id, batchResult)
    }
  }
    .runForeach(p => println(s"StartId: ${p.head.id}"))
    .onComplete {
      case Success(_) =>
        println("Success.")
        system.terminate()
        System.exit(0)
      case Failure(cause) =>
        println(s"Failure. $cause")
        system.terminate()
        System.exit(1)
    }

  def getProducts(startId: Long, batchSize: Int) = {
    val connection = dataSource.getConnection()
    val rs = connection.prepareStatement(
      s"""
         |SELECT id, name
         |FROM product
         |WHERE id > $startId
         |ORDER BY id
         |LIMIT $batchSize""".stripMargin
    ).executeQuery()

    val result = scala.collection.mutable.ArrayBuffer.empty[Product]
    while (rs.next()) {
      result += Product(rs.getLong("id"), rs.getString("name"))
    }

    connection.close()
    result.toList
  }
}
