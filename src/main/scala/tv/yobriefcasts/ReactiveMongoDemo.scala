package tv.yobriefcasts

import reactivemongo.api.MongoConnection
import reactivemongo.bson._
import concurrent.ExecutionContext.Implicits._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import util.{Success, Failure}
import concurrent.Future

object ReactiveMongoDemo extends App {

  val connection = MongoConnection(List("localhost:27017"))
  val database = connection("reactivemongodemo")
  val stockitems = database("stockitems")

  /**
   * We want to execute these commands in sequence so use a
   * for-comprehension to achieve this
   */
  for (
    _ <- dropDatabase;
    _ <- seed;
    _ <- insertSingle;
    _ <- query;
    _ <- updateSingle;
    _ <- delete
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

  /**
   * Updates a single item by decrementing its quantity.  If
   * successful it will fetch the item and print out its quantity
   */
  def updateSingle = {
    println("Updating document")

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

}


