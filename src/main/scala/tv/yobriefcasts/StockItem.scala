package tv.yobriefcasts

import reactivemongo.bson.{BSONLong, BSONString, BSONDocument, BSONObjectID}
import reactivemongo.bson.handlers.{BSONWriter, BSONReader}

case class StockItem(id: Option[BSONObjectID], name: String, quantity: Long)

object StockItem {

  implicit object StockItemBSONReader extends BSONReader[StockItem] {
    def fromBSON(doc: BSONDocument): StockItem = {
      val document = doc.toTraversable
      StockItem(
        document.getAs[BSONObjectID]("_id"),
        document.getAs[BSONString]("name").get.value,
        document.getAs[BSONLong]("quantity").get.value
      )
    }
  }

  implicit object StockItemBSONWriter extends BSONWriter[StockItem]{
    def toBSON(item: StockItem): BSONDocument = {
      BSONDocument(
        "_id" -> item.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(item.name),
        "quantity" -> BSONLong(item.quantity)
      )
    }
  }
}
