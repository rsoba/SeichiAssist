package com.github.unchama.seichiassist.subsystems.awayscreenname

import cats.effect.SyncIO
import com.github.unchama.datarepository.bukkit.player.BukkitRepositoryControls
import com.github.unchama.datarepository.template.RepositoryDefinition
import com.github.unchama.seichiassist.meta.subsystem.Subsystem
import com.github.unchama.seichiassist.subsystems.awayscreenname.application.repository.IdleMinuteRepositoryDefinitions
import com.github.unchama.seichiassist.subsystems.awayscreenname.domain.IdleMinute
import org.bukkit.entity.Player

import java.util.UUID

trait System[F[_]] extends Subsystem[F] {

  val api: AwayScreenNameAPI[F]

}

object System {

  def wired[F[_]]: SyncIO[System[F]] = {
    for {
      idleMinuteRepositoryControls <- BukkitRepositoryControls.createHandles(
        RepositoryDefinition
          .Phased
          .TwoPhased(
            IdleMinuteRepositoryDefinitions.initialization[SyncIO, Player],
            IdleMinuteRepositoryDefinitions.finalization[SyncIO, Player]
          )
      )
    } yield {
      idleMinuteRepositoryControls.repository
      new System[F] {
        override val api: AwayScreenNameAPI[F] = new AwayScreenNameAPI[F] {

          /**
           * @return 指定したUUIDプレイヤーの[[IdleMinute]]を返す作用
           */
          override def idleTime(uuid: UUID): F[IdleMinute] = ???
        }
      }
    }
  }

}
