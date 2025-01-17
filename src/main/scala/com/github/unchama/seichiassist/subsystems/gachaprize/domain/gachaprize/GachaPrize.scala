package com.github.unchama.seichiassist.subsystems.gachaprize.domain.gachaprize

import com.github.unchama.seichiassist.subsystems.gachaprize.domain.{
  CanBeSignedAsGachaPrize,
  GachaProbability
}
import com.github.unchama.generic.Cloneable

import com.github.unchama.seichiassist.subsystems.gachaprize.domain.gachaevent.GachaEventName

/**
 * @param itemStack ガチャで排出されるアイテム。
 * @param probability ガチャで排出される確率
 * @param signOwner 記名する場合はtrueにしてください
 * @param gachaEventName ガチャイベントで排出されるアイテムの場合は設定してください。
 *                       `None`の場合は通常排出アイテムとして扱います。
 */
case class GachaPrize[ItemStack: Cloneable](
  itemStack: ItemStack,
  probability: GachaProbability,
  signOwner: Boolean,
  id: GachaPrizeId,
  gachaEventName: Option[GachaEventName]
) {

  def materializeWithOwnerSignature(
    ownerName: String
  )(implicit sign: CanBeSignedAsGachaPrize[ItemStack]): ItemStack = {
    if (signOwner) sign.signWith(ownerName)(this)
    else Cloneable[ItemStack].clone(itemStack)
  }

}
