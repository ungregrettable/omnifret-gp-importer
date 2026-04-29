package com.omnifret.gplayer.core

interface IGPlayerEnum {
    val value: Int
}


interface IGPlayerEnumCompanion<T> {
    val values:Array<T>
    fun fromValue(value:Double): T
}
