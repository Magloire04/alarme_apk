package com.bytechnum.reveil.domain.model

/** Sonnerie en cours, conservée pour reprendre au bon palier si le service est tué. */
data class RingingState(val alarmId: Int, val firedAt: Long)
