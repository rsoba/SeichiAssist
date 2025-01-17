package com.github.unchama.seichiassist.subsystems.minestack.bukkit

import cats.effect.IO
import com.github.unchama.contextualexecutor.ContextualExecutor
import com.github.unchama.contextualexecutor.builder.ParserResponse.{failWith, succeedWith}
import com.github.unchama.contextualexecutor.builder.Parsers
import com.github.unchama.contextualexecutor.executors.BranchedExecutor
import com.github.unchama.menuinventory.router.CanOpen
import com.github.unchama.seichiassist.commands.contextual.builder.BuilderTemplates.playerCommandBuilder
import com.github.unchama.seichiassist.menus.minestack.{
  CategorizedMineStackMenu,
  MineStackMainMenu
}
import com.github.unchama.seichiassist.subsystems.minestack.MineStackAPI
import com.github.unchama.seichiassist.subsystems.minestack.domain.minestackobject.MineStackObjectCategory
import com.github.unchama.targetedeffect.TargetedEffect.emptyEffect
import com.github.unchama.targetedeffect.commandsender.MessageEffect
import com.github.unchama.targetedeffect.{DeferredEffect, SequentialEffect}
import org.bukkit.ChatColor._
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object MineStackCommand {
  def executor(
    implicit ioCanOpenCategorizedMenu: IO CanOpen CategorizedMineStackMenu,
    ioCanOpenMinestackMainMenu: IO CanOpen MineStackMainMenu.type,
    mineStackAPI: MineStackAPI[IO, Player, ItemStack]
  ): TabExecutor =
    BranchedExecutor(
      Map(
        "on" -> ChildExecutors.setAutoCollectionExecutor(isItemCollectedAutomatically = true),
        "off" -> ChildExecutors.setAutoCollectionExecutor(isItemCollectedAutomatically = false),
        "open" -> ChildExecutors.openCategorizedMineStackMenu,
        "store-all" -> ChildExecutors.storeEverythingInInventory
      )
    ).asNonBlockingTabExecutor()

  object ChildExecutors {

    def setAutoCollectionExecutor(
      isItemCollectedAutomatically: Boolean
    )(implicit mineStackAPI: MineStackAPI[IO, Player, ItemStack]): ContextualExecutor =
      playerCommandBuilder
        .execution { _ =>
          IO {
            SequentialEffect(
              DeferredEffect {
                IO(mineStackAPI.setAutoMineStack(isItemCollectedAutomatically))
              },
              if (isItemCollectedAutomatically)
                MessageEffect("MineStack自動収集をONにしました。")
              else
                MessageEffect("MineStack自動収集をOFFにしました。")
            )
          }
        }
        .build()

    def openCategorizedMineStackMenu(
      implicit ioCanOpenCategorizedMenu: IO CanOpen CategorizedMineStackMenu,
      ioCanOpenMinestackMainMenu: IO CanOpen MineStackMainMenu.type
    ): ContextualExecutor =
      playerCommandBuilder
        .argumentsParsers(
          List(
            Parsers
              .closedRangeInt(0, Int.MaxValue, MessageEffect("カテゴリは0以上の値を入力してください"))
              .andThen(_.flatMap { _categoryValue =>
                val categoryValue = _categoryValue.asInstanceOf[Int]
                MineStackObjectCategory.fromSerializedValue(categoryValue - 1) match {
                  case Some(category)             => succeedWith(category)
                  case None if categoryValue == 0 => succeedWith(emptyEffect)
                  case None                       => failWith("指定されたカテゴリは存在しません。")
                }
              }),
            Parsers.closedRangeInt(0, Int.MaxValue, MessageEffect("ページ数は0以上の値を指定してください。"))
          )
        )
        .execution { context =>
          val categoryValue = context.args.parsed(1).toString.toInt
          IO.pure(if (categoryValue == 0) {
            ioCanOpenMinestackMainMenu.open(MineStackMainMenu)
          } else {
            ioCanOpenCategorizedMenu.open(
              new CategorizedMineStackMenu(
                context.args.parsed.head.asInstanceOf[MineStackObjectCategory],
                categoryValue - 1
              )
            )
          })
        }
        .build()

    import cats.implicits._

    def storeEverythingInInventory(
      implicit mineStackAPI: MineStackAPI[IO, Player, ItemStack]
    ): ContextualExecutor =
      playerCommandBuilder
        .execution { context =>
          for {
            player <- IO(context.sender)
            inventory <- IO(player.getInventory)
            targetIndexes <- inventory.getContents.toList.zipWithIndex.traverse {
              case (itemStack, index) if itemStack != null =>
                mineStackAPI
                  .mineStackRepository
                  .tryIntoMineStack(player, itemStack, itemStack.getAmount)
                  .map(Option.when(_)(index))
              case _ => IO.pure(None)
            }
            _ <- IO(targetIndexes.foreach(_.foreach(index => inventory.clear(index))))
          } yield MessageEffect(s"${YELLOW}インベントリの中身をすべてマインスタックに収納しました。")
        }
        .build()

  }

}
