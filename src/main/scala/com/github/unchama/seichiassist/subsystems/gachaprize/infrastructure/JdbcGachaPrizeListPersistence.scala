package com.github.unchama.seichiassist.subsystems.gachaprize.infrastructure

import cats.effect.Sync
import com.github.unchama.generic.serialization.SerializeAndDeserialize
import com.github.unchama.seichiassist.subsystems.gachaprize.domain._
import com.github.unchama.seichiassist.subsystems.gachaprize.domain.gachaevent.GachaEventName
import com.github.unchama.seichiassist.subsystems.gachaprize.domain.gachaprize.{
  GachaPrize,
  GachaPrizeId
}
import scalikejdbc._

class JdbcGachaPrizeListPersistence[F[_]: Sync, ItemStack](
  implicit serializeAndDeserialize: SerializeAndDeserialize[Nothing, ItemStack]
) extends GachaPrizeListPersistence[F, ItemStack] {

  override def list: F[Vector[GachaPrize[ItemStack]]] = {
    Sync[F].delay {
      DB.readOnly { implicit session =>
        sql"""SELECT gachadata.id, gachadata.probability, gachadata.itemstack, gacha_events.event_name FROM gachadata
             | LEFT OUTER JOIN gacha_events ON gachadata.event_id = gacha_events.id
             | UNION
             | SELECT gachadata.id, gachadata.probability, gachadata.itemstack, gacha_events.event_name FROM gachadata
             | RIGHT OUTER JOIN gacha_events ON gachadata.event_id = gacha_events.id"""
          .stripMargin
          .map { rs =>
            val probability = rs.double("probability")
            // TODO ガチャアイテムに対して記名を行うかどうかを確率に依存すべきではない
            serializeAndDeserialize
              .deserialize(rs.string("itemstack"))
              .map { itemStack =>
                gachaprize.GachaPrize(
                  itemStack,
                  GachaProbability(probability),
                  probability < 0.1,
                  GachaPrizeId(rs.int("id")),
                  rs.stringOpt("event_name").map(GachaEventName)
                )
              }
              .merge
          }
          .toList()
          .apply()
          .toVector
      }
    }
  }

  override def set(gachaPrizesList: Vector[GachaPrize[ItemStack]]): F[Unit] = {
    Sync[F].delay {
      DB.localTx { implicit session =>
        sql"truncate table gachadata".execute().apply()
        val batchParams = gachaPrizesList.map { gachaPrize =>
          Seq(
            gachaPrize.id.id,
            gachaPrize.probability.value,
            serializeAndDeserialize.serialize(gachaPrize.itemStack),
            gachaPrize.gachaEventName.map(_.name)
          )
        }
        sql"insert into gachadata values (?,?,?,?)".batch(batchParams).apply[List]()
      }
    }
  }

  override def addMineStackGachaObject(id: GachaPrizeId, objectName: String): F[Unit] =
    Sync[F].delay {
      DB.localTx { implicit session =>
        sql"INSERT INTO mine_stack_gacha_objects (id, mine_stack_object_name) VALUES (${id.id}, $objectName)"
          .execute()
          .apply()
      }
    }

  override def deleteMineStackGachaObject(id: GachaPrizeId): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"DELETE FROM mine_stack_gacha_objects WHERE id = ${id.id}".execute().apply()
    }
  }

}