package com.github.meypod.al_azan.core.domain.util

val charPool by lazy { ('a'..'z') + ('A'..'Z') + ('0'..'9') }

fun randomString(length: Int): String = (1..length).map { charPool.random() }.joinToString("")
