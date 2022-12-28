package com.github.unchama.seichiassist.subsystems.lastquit

import com.github.unchama.seichiassist.subsystems.lastquit.domain.LastQuitDateTime

import java.util.UUID

trait LastQuitWriteAPI[F[_]] {

  /**
   * 最終ログアウト日時を現在の時刻で更新します。
   */
  def updateLastLastQuitDateTimeNow(uuid: UUID): F[Unit]

}

object LastQuitWriteAPI {

  def apply[F[_]](implicit ev: LastQuitWriteAPI[F]): LastQuitWriteAPI[F] = ev

}

trait LastQuitReadAPI[F[_]] {

  /**
   * 最終ログアウト日時を取得します。
   */
  def lastQuitDateTime(uuid: UUID): F[Option[LastQuitDateTime]]

}

object LastQuitReadAPI {

  def apply[F[_]](implicit ev: LastQuitReadAPI[F]): LastQuitReadAPI[F] = ev

}

trait LastQuitAPI[F[_]] extends LastQuitReadAPI[F] with LastQuitWriteAPI[F]
