package com.zafaris.whichweek

import java.time.LocalDate

const val SHARED_PREFS = "com.zafaris.whichweek"

//const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val AD_UNIT_ID = "ca-app-pub-4222736956813262/3450822903"

//TODO: Update these values every year
const val FIRST_YEAR = 2020
const val SECOND_YEAR = FIRST_YEAR + 1

const val FIRST_WEEK_ISO = 36
const val SCHOOL_WEEKS_IN_FIRST_YEAR = 18
const val LAST_WEEK_ISO = 29

val FIRST_SCHOOL_DAY: LocalDate? = LocalDate.of(2020, 9, 1)
val LAST_SCHOOL_DAY: LocalDate? = LocalDate.of(2021, 7, 21)

val HOLIDAYS_FIRST_YEAR = arrayListOf(44, 52, 53)
val HOLIDAYS_SECOND_YEAR = arrayListOf(7, 14, 15, 22)
val HOLIDAYS_LIST = HOLIDAYS_FIRST_YEAR + HOLIDAYS_SECOND_YEAR