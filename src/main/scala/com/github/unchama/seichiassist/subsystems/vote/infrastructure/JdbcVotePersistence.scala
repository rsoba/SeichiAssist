package com.github.unchama.seichiassist.subsystems.vote.infrastructure

import cats.effect.Sync
import com.github.unchama.seichiassist.subsystems.vote.domain._
import scalikejdbc.{DB, scalikejdbcSQLInterpolationImplicitDef}

import java.util.UUID

class JdbcVotePersistence[F[_]: Sync] extends VotePersistence[F] {

  // NOTE: 連続投票許容幅を変更する場合はここを変更してください。
  private val chainVoteAllowableWidth = 4

  def createPlayerData(uuid: UUID): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"""INSERT IGNORE INTO vote 
           | (uuid, vote_number, chain_vote_number, effect_point, given_effect_point, last_vote)
           | VALUES
           | (${uuid.toString}, 0, 0, 0, 0, NULL)""".stripMargin.execute().apply()
    }
  }

  override def voteCounterIncrement(playerName: PlayerName): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"UPDATE vote SET vote_number = vote_number + 1 WHERE uuid = (SELECT uuid FROM playerdata WHERE name = ${playerName.name})"
        .execute()
        .apply()
    }
  }

  override def voteCounter(uuid: UUID): F[VoteCounter] = Sync[F].delay {
    DB.readOnly { implicit session =>
      val votePoint = sql"SELECT vote_number FROM vote WHERE uuid = ${uuid.toString}"
        .map(_.int("vote_number"))
        .single()
        .apply()
        .get
      VoteCounter(votePoint)
    }
  }

  override def updateChainVote(playerName: PlayerName): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      /*
        NOTE: 最終投票日時より(連続投票許容幅 - 1)した日時よりも
          小さかった場合に連続投票を0に戻します。
       */
      sql"""UPDATE vote SET chain_vote_number = 
           | CASE WHEN DATEDIFF(last_vote, NOW()) <= ${-chainVoteAllowableWidth - 1}
           | THEN 0 
           | ELSE chain_vote_number + 1 
           | END,
           | last_vote = NOW()
           | WHERE uuid = (SELECT uuid FROM playerdata WHERE name = ${playerName.name})"""
        .stripMargin
        .execute()
        .apply()
    }
  }

  override def chainVoteDays(uuid: UUID): F[ChainVoteDayNumber] = Sync[F].delay {
    DB.readOnly { implicit session =>
      val chainVoteDays = sql"SELECT chain_vote_number FROM vote WHERE uuid = ${uuid.toString}"
        .map(_.int("chain_vote_number"))
        .single()
        .apply()
        .get
      ChainVoteDayNumber(chainVoteDays)
    }
  }

  override def increaseEffectPoints(uuid: UUID, effectPoint: EffectPoint): F[Unit] =
    Sync[F].delay {
      DB.localTx { implicit session =>
        sql"UPDATE vote SET effect_point = effect_point + ${effectPoint.value} WHERE uuid = ${uuid.toString}"
          .execute()
          .apply()
      }
    }

  override def decreaseEffectPoints(uuid: UUID, effectPoint: EffectPoint): F[Unit] =
    Sync[F].delay {
      DB.localTx { implicit session =>
        sql"UPDATE vote SET effect_point = effect_point - ${effectPoint.value} WHERE uuid = ${uuid.toString}"
          .execute()
          .apply()
      }
    }

  override def effectPoints(uuid: UUID): F[EffectPoint] = Sync[F].delay {
    DB.readOnly { implicit session =>
      val effectPoints = sql"SELECT effect_point FROM vote WHERE uuid = ${uuid.toString}"
        .map(_.int("effect_point"))
        .single()
        .apply()
        .get
      EffectPoint(effectPoints)
    }
  }

  override def increaseVoteBenefits(uuid: UUID, benefit: VoteBenefit): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"UPDATE vote SET given_effect_point = given_effect_point + ${benefit.value} WHERE uuid = ${uuid.toString}"
        .execute()
        .apply()
    }
  }

  override def receivedVoteBenefits(uuid: UUID): F[VoteBenefit] = Sync[F].delay {
    DB.readOnly { implicit session =>
      val benefits = sql"SELECT given_effect_point FROM vote WHERE uuid = ${uuid.toString}"
        .map(_.int("given_effect_point"))
        .single()
        .apply()
        .get
      VoteBenefit(benefits)
    }
  }
}
