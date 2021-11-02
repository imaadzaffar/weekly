package com.zafaris.whichweek

import java.time.LocalDate

const val SHARED_PREFS = "com.zafaris.whichweek"

//const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val AD_UNIT_ID = "ca-app-pub-4222736956813262/3450822903"

//TODO: Update these values every year
const val FIRST_YEAR = 2021
const val SECOND_YEAR = FIRST_YEAR + 1

const val FIRST_WEEK_ISO = 36
const val SCHOOL_WEEKS_IN_FIRST_YEAR = 17
const val LAST_WEEK_ISO = 29

val FIRST_SCHOOL_DAY: LocalDate? = LocalDate.of(2021, 9, 6)
val LAST_SCHOOL_DAY: LocalDate? = LocalDate.of(2022, 7, 22)

val HOLIDAYS_FIRST_YEAR = arrayListOf(43, 51, 52)
val HOLIDAYS_SECOND_YEAR = arrayListOf(8, 15, 16, 22)
val HOLIDAYS_LIST = HOLIDAYS_FIRST_YEAR + HOLIDAYS_SECOND_YEAR