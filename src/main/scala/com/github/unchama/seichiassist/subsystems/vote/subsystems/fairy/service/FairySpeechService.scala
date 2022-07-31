package com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.service

import cats.effect.Sync
import cats.implicits.catsSyntaxFlatMapOps
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.FairySpeechGateway
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.property.FairyMessage

class FairySpeechService[F[_]: Sync](gateway: FairySpeechGateway[F]) {

  def makeSpeech(fairyMessage: FairyMessage, fairyPlaySound: Boolean): F[Unit] = {
    gateway.sendMessage(fairyMessage) >> Sync[F].whenA(fairyPlaySound)(gateway.playSpeechSound)
  }

}
