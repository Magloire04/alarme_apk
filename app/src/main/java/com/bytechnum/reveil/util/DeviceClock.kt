package com.bytechnum.reveil.util

import com.bytechnum.reveil.domain.port.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** Heure du téléphone, relue avec le fuseau courant à chaque appel. */
class DeviceClock @Inject constructor() : Clock {
    override fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
}
