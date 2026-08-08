package com.pvlm.rorafreeze.domain.model

data class FreezeMode(
    val id: String,
    val name: String,
    val packageNames: List<String>
)