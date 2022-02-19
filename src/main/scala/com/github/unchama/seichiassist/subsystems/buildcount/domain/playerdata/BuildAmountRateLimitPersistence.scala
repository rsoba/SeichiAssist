package com.github.unchama.seichiassist.subsystems.buildcount.domain.playerdata

import com.github.unchama.generic.RefDict
import com.github.unchama.seichiassist.subsystems.buildcount.domain.BuildAmountPersistenceRecord

import java.util.UUID

trait BuildAmountRateLimitPersistence[F[_]] extends RefDict[F, UUID, BuildAmountPersistenceRecord]
