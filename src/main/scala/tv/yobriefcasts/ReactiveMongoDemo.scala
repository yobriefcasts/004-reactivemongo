package tv.yobriefcasts

import reactivemongo.api.MongoConnection
import reactivemongo.bson._
import concurrent.ExecutionContext.Implicits._
import util.{Success, Failure}
import concurrent.{Await, Future}
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import java.io.{FileOutputStream, File}
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.indexes.{IndexType, Index}

object ReactiveMongoDemo extends App {

  val connection = MongoConnection(List("localhost:27017"))
  val database   = connection("reactivemongodemo")
  val stockitems = database("stockitems")
  val images     = new GridFS(database, "images")

  stockitems.indexesManager.create(
    Index("name" -> IndexType.Ascending :: Nil, unique = true)
  )

  /**
   * We want to execute these commands in sequence so use a
   * for-comprehension to achieve this
   */
  for (
    _ <- dropDatabase;
    _ <- seed;
    _ <- insertSingle;
    _ <- breakIndex;
    _ <- query;
    _ <- updateSingle;
    _ <- delete;
    _ <- insertCaseClass;
    _ <- readCaseClass;
    _ <- insertFile;
    _ <- getFile
  ) yield println("Complete")

  /**
   * Drops the database entirely
   */
  def dropDatabase = {
    println("Dropping database")

    database.drop andThen { case _ =>
      println("Dropped Database")
    }
  }

  /**
   * Seeds the database with some example inventory items
   * Inserts each document in turn rather than by bulk
   */
  def seed = {
    println("Inserting documents")

    val documents = BSONDocument(
      "name" -> BSONString("Toothpaste"),
      "quantity" -> BSONLong(1)
    ) :: BSONDocument(
      "name" -> BSONString("Toilet Paper"),
      "quantity" -> BSONLong(3)
    )  :: BSONDocument(
      "name" -> BSONString("Toothbrush"),
      "quantity" -> BSONLong(3)
    )  :: BSONDocument(
      "name" -> BSONString("Helicopter"),
      "quantity" -> BSONLong(7)
    ) :: Nil

    Future.sequence(documents.map {
      stockitems.insert(_)
    }) andThen { case _ =>
      println("Inserted documents")
    }
  }

  /**
   * Inserts a single record into the collection.
   */
  def insertSingle = {

    val document = BSONDocument(
      "name" -> BSONString("Face Cloth"),
      "quantity" -> BSONLong(5)
    )

    stockitems.insert(document)
  }

  def breakIndex = {
    println("Breaking Index")
    val document = BSONDocument(
      "name" -> BSONString("Helicopter"),
      "quantity" -> BSONLong(7)
    )

    stockitems.insert(document) recover { case error =>
      println(s"As expected we got an error: ${error.getMessage}")
    }
  }

  /**
   * Updates a single item by decrementing its quantity.  If
   * successful it will fetch the item and print out its quantity
   */
  def updateSingle = {
    println("Updating document")

    implicit val reader = handlers.DefaultBSONHandlers.DefaultBSONDocumentReader

    val query = BSONDocument("name" -> BSONString("Helicopter"))
    val update = BSONDocument(
      "$inc" -> BSONDocument("quantity" -> BSONLong(-1))
    )

    stockitems.update(query, update, multi = false) andThen {
      case Failure(_) => println("Failed update")
      case Success(_) => {
        println("Document updated")

        stockitems.find(BSONDocument(
          "name" -> BSONString("Helicopter")
        )).headOption onSuccess { case Some(result) =>
          val quantity = result.getAs[BSONLong]("quantity").get.value
          println(s"New Quantity for Helicopter ${quantity}")
        }
      }
    }
  }

  /**
   * Runs a query for documents whose quantity is greater than
   * two and print outs the size of the cursor
   */
  def query = {
    println("Querying database")

    implicit val reader = handlers.DefaultBSONHandlers.DefaultBSONDocumentReader

    stockitems.find(
      BSONDocument( "quantity" -> BSONDocument("$gt" -> BSONLong(2)) )
    ).toList.map { found =>
      println(s"Found ${found.size} documents")
    }
  }

  /**
   * Deletes a single record from the database
   */
  def delete = {
    stockitems.remove(BSONDocument(
      "name" -> BSONString("Helicopter")
    ), firstMatchOnly = true) andThen { case _ =>
      println("Item deleted")
    }
  }

  def insertCaseClass = {
    import StockItem._
    stockitems.insert(StockItem(None, "Asprin", 30)) andThen { case _ =>
      println("Inserted Case Class")
    }
  }

  def readCaseClass = {
    implicit val reader = StockItem.StockItemBSONReader
    stockitems.find(
      BSONDocument("name" -> BSONString("Asprin"))
    ).headOption andThen { case Success(Some(item)) =>
      println(s"${item.name} - ${item.quantity}")
    }
  }

  def insertFile = {
    import reactivemongo.api.gridfs.Implicits._

    val file = new File("image.png")
    val enum = Enumerator.fromFile(file)

    images.save(enum, DefaultFileToSave(file.getName)) andThen { case t =>
      println(s"File ${if(t.isFailure) "not" else ""} saved")
    }
  }

  def getFile = {

    println("Getting file")

    import reactivemongo.api.gridfs.Implicits._

    images.find(BSONDocument("filename" -> BSONString("image.png"))).headOption.map { maybeFile =>
      maybeFile.map { file =>

        val filename = s"${file.id.asInstanceOf[BSONObjectID].stringify}.png"
        val stream = new FileOutputStream(filename)

        images.readToOutputStream(file, stream) andThen { case _ =>
          print(s"Written $filename")
          stream.close
        }
      } getOrElse {
        println(":(")
      }
    }
  }
}


