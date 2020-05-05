package com.zafaris.whichweek

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.muddzdev.styleabletoast.StyleableToast
import org.threeten.extra.YearWeek
import java.io.IOException
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var fullDateTv: TextView
    private lateinit var currentWeekTv: TextView
    private lateinit var nextWeekTv: TextView
    private lateinit var changeFormatSwitch: Switch

    private lateinit var adContainerView: FrameLayout
    private lateinit var adView: AdView
    private var adViewLayoutComplete = false
    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = adContainerView.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private lateinit var today: LocalDate
    private var dayOfWeek = 0
    private var currentYear = 0
    private var currentWeekISO = 0

    private var weekList: ArrayList<Int>? = null
    private var weekFormat = 0
    private var weekFormatList: ArrayList<String>? = arrayListOf("A", "B")
    private var currentWeekType = 0
    private var nextWeekType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideStatusBar()
        animateBackground()

        fullDateTv = findViewById(R.id.fullDateTv)
        currentWeekTv = findViewById(R.id.currentWeekTv)
        nextWeekTv = findViewById(R.id.nextWeekTv)
        changeFormatSwitch = findViewById(R.id.changeFormatSwitch)
        changeFormatSwitch.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                false -> changeWeekFormat(1)
                true -> changeWeekFormat(2)
            }
        }

        prefs = getSharedPreferences("com.zafaris.whichweek", Context.MODE_PRIVATE)

        if (prefs.getBoolean("firstTime", true)) {
            prefs.edit().putBoolean("firstTime", false).apply()
            showStartDialog()
            generateWeeksList()
        } else {
            getWeeksList()
        }

        // Week A/B or 1/2
        weekFormat = prefs.getInt("weekFormat", 1)
        // Toggle switch if week format is 1/2
        if (weekFormat == 2) {
            changeFormatSwitch.isChecked = true
            weekFormatList = arrayListOf("1", "2")
        }

        getDateInfo()
        val weeksText = generateWeeksText()
        currentWeekTv.text = weeksText[0]
        nextWeekTv.text = weeksText[1]
        
        setupBannerAd()
    }

    private fun showStartDialog() {
        AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage("This app was made according to the King Edward VI Birmingham, UK school timetables. " +
                        "\n\nIf you do not attend one of these schools, the week types and holidays shown might not be correct for you.")
                .setPositiveButton("I understand") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun getWeeksList() {
        try {
            weekList = ObjectSerializer.deserialize(prefs.getString("weekList", "")) as ArrayList<Int>
        } catch (e: IOException) {
            e.printStackTrace()
            StyleableToast.makeText(this, "Error loading weeks...", Toast.LENGTH_SHORT, R.style.ErrorToast).show()
        }
    }

    private fun generateWeeksList() {
        weekList = ArrayList()

        // School year starts on week A
        var tmpWeekType = 1
        var tmpYear: Int

        Log.i("HOLIDAYS_LIST", HOLIDAYS_LIST.toString())

        // Assigning values to each week of the year
        for (i in 0..52) {
            var tmpWeekISO: Int
            if (i < WEEKS_IN_FIRST_YEAR) {
                tmpWeekISO = FIRST_WEEK_ISO + i
                tmpYear = FIRST_YEAR
            } else {
                tmpWeekISO = (i - WEEKS_IN_FIRST_YEAR) + 1
                tmpYear = SECOND_YEAR
            }

            // Toggles week type A, B, A, etc...
            when {
                tmpYear == SECOND_YEAR && tmpWeekISO > LAST_WEEK_ISO -> weekList!!.add(0)
                HOLIDAYS_LIST.contains(tmpWeekISO) -> weekList!!.add(0)
                tmpWeekType == 1 -> { weekList!!.add(tmpWeekType)
                    tmpWeekType = 2 }
                tmpWeekType == 2 -> { weekList!!.add(tmpWeekType)
                    tmpWeekType = 1 }
            }

            Log.i("Weeks List", "Year: $tmpYear, Week: $tmpWeekISO, Week Type: ${weekList!![i]}")
        }
        try {
            prefs.edit().putString("weekList", ObjectSerializer.serialize(weekList)).apply()
        } catch (e: IOException) {
            e.printStackTrace()
            StyleableToast.makeText(this, "Error saving weeks...", Toast.LENGTH_SHORT, R.style.ErrorToast).show()
        }
    }

    private fun generateWeeksText(): Array<String?> {
        val weeksText = arrayOfNulls<String>(2)

        val week1 = weekFormatList!![0]
        val week2 = weekFormatList!![1]

        if (currentYear == FIRST_YEAR) {
            // Checks if it is the last week of the year
            if (currentWeekISO == WEEKS_IN_FIRST_YEAR) {
                currentWeekType = weekList!![WEEKS_IN_FIRST_YEAR - 1]
                nextWeekType = weekList!![(WEEKS_IN_FIRST_YEAR - 1) + 1]
            } else {
                currentWeekType = weekList!![currentWeekISO - FIRST_WEEK_ISO]
                nextWeekType = weekList!![(currentWeekISO - FIRST_WEEK_ISO) + 1]
            }
        } else {
            currentWeekType = weekList!![currentWeekISO + WEEKS_IN_FIRST_YEAR - 1]
            nextWeekType = weekList!![(currentWeekISO + WEEKS_IN_FIRST_YEAR - 1) + 1]
        }

        // Generate current week text - 0 [Holiday], 1 [Week A/1] or 2 [Week B/2]
        when {
            currentWeekType == 0 -> weeksText[0] = "Enjoy the holidays!"
            dayOfWeek == 7 || dayOfWeek == 1 -> weeksText[0] = "It's the weekend!"
            currentWeekType == 1 -> weeksText[0] = "It is Week $week1"
            currentWeekType == 2 -> weeksText[0] = "It is Week $week2"
        }
        // Generate next week text - 0 [Holiday], 1 [Week A/1] or 2 [Week B/2]
        when (nextWeekType) {
            0 -> weeksText[1] = "No school next week!"
            1 -> weeksText[1] = "Next week will be Week $week1"
            2 -> weeksText[1] = "Next week will be Week $week2"
        }
        return weeksText
    }

    private fun getDateInfo() {
        val calendar = Calendar.getInstance()
        currentYear = calendar[Calendar.YEAR]
        dayOfWeek = calendar[Calendar.DAY_OF_WEEK]

        // Full date text
        val date = calendar.time
        val fullDate = DateFormat.getDateInstance(DateFormat.FULL).format(date)
        fullDateTv.text = fullDate

        // Week of year ISO
        val zoneId = ZoneId.of("Europe/London")
        today = LocalDate.now(zoneId)
        currentWeekISO = YearWeek.from(today).week
    }
    
    private fun getWeekType(date: LocalDate): Int {
        val year = date.year
        val weekISO = YearWeek.from(date).week
        val weekType: Int

        weekType = if (year == FIRST_YEAR) {
            // If last week of the year
            if (weekISO == WEEKS_IN_FIRST_YEAR) {
                weekList!![WEEKS_IN_FIRST_YEAR - 1]
            } else {
                weekList!![weekISO - FIRST_WEEK_ISO]
            }
        } else {
            weekList!![weekISO + WEEKS_IN_FIRST_YEAR - 1]
        }
        return weekType
    }

    private fun changeWeekFormat(weekFormat: Int) {
        weekFormatList = if (weekFormat == 1) {
            arrayListOf("A", "B")
        } else {
            arrayListOf("1", "2")
        }

        prefs.edit().putInt("weekFormat", weekFormat).apply()
        val weeksText = generateWeeksText()
        currentWeekTv.text = weeksText[0]
        nextWeekTv.text = weeksText[1]
    }
    
    fun tapScreenAnimation(view: View?) {
        val centreLayout = findViewById<ConstraintLayout>(R.id.centreLayout)
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce_animation)
        centreLayout.startAnimation(bounce)
    }

    fun tapDateBtn(view: View?) {
        val builder = MaterialDatePicker.Builder.datePicker()
        val datePicker = builder.build()
        datePicker.show(supportFragmentManager, datePicker.toString())

        datePicker.addOnPositiveButtonClickListener {
            val epochDay = it / 1000 / 3600 / 24 // it is epoch time stamp in ms
            val selectedDate = LocalDate.ofEpochDay(epochDay)

            // Check if selected date is within the date range of the school year
            if (selectedDate.isAfter(FIRST_SCHOOL_DAY?.minusDays(1))
                    && selectedDate.isBefore(LAST_SCHOOL_DAY?.plusDays(1))) {

                val selectedDateFormatted = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                val selectedWeekType = getWeekType(selectedDate)
                val message: String
                val verb: String = when {
                    selectedDate.isBefore(today) -> "was"
                    selectedDate.isAfter(today) -> "will be"
                    else -> "is"
                }
                message = if (selectedWeekType == 0) {
                    "$selectedDateFormatted $verb a holiday"
                } else {
                    val selectedWeekText = weekFormatList?.get(selectedWeekType - 1)
                    "$selectedDateFormatted $verb Week $selectedWeekText"
                }
                StyleableToast.makeText(this, message, Toast.LENGTH_LONG, R.style.CustomToast).show()
            } else {
                val message = "Date is not in the school year"
                StyleableToast.makeText(this, message, Toast.LENGTH_LONG, R.style.ErrorToast).show()
            }
        }
    }

    fun tapShareBtn(view: View?) {
        var message = "It is Week $currentWeekType :)"
        if (currentWeekType == 0) {
            message = "Enjoy the holidays :)"
        }
        message +=  "\n\nNever forget which week it is again with the Week.ly app! " +
                "Download now: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID

        val shareIntent = Intent()
        shareIntent.type = "text/plain"
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
        startActivity(Intent.createChooser(shareIntent, "Share with: "))
    }

    fun tapSendEmail(view: View?) {
        val email = "iszaffar.apps@gmail.com"
        val subject = "Week.ly App Feedback"
        val chooserTitle = "Email with: "
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email?subject=$subject"))
        startActivity(Intent.createChooser(emailIntent, chooserTitle))
    }

    private fun hideStatusBar() {
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun animateBackground() {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.layout)
        val animationDrawable = constraintLayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()
    }

    private fun setupBannerAd() {
        adContainerView = findViewById(R.id.ad_view_container)
        MobileAds.initialize(this) {}
        val testDeviceIds = listOf("6D2382FDF2727AA22ECCD727F5429CD7")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)

        adView = AdView(this)
        adContainerView.addView(adView)
        adContainerView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!adViewLayoutComplete) {
                adViewLayoutComplete = true
                loadBannerAd()
            }
        }
    }

    private fun loadBannerAd() {
        adView.adUnitId = AD_UNIT_ID
        adView.adSize = adSize
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    companion object {
        fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
            val win = activity.window
            val winParams = win.attributes
            if (on) {
                winParams.flags = winParams.flags or bits
            } else {
                winParams.flags = winParams.flags and bits.inv()
            }
            win.attributes = winParams
        }

        //TODO: Update these values every year
        const val FIRST_YEAR = 2019
        const val FIRST_WEEK_ISO = 36
        const val LAST_WEEK_ISO = 29
        const val WEEKS_IN_FIRST_YEAR = 17
        private val HOLIDAYS_FIRST_YEAR = arrayListOf(44, 52)
        private val HOLIDAYS_SECOND_YEAR = arrayListOf(1, 8, 15, 16, 22)
        private val FIRST_SCHOOL_DAY: LocalDate? = LocalDate.of(2019, 9, 2)
        private val LAST_SCHOOL_DAY: LocalDate? = LocalDate.of(2020, 7, 17)

        const val SECOND_YEAR = FIRST_YEAR + 1
        const val WEEKS_IN_SECOND_YEAR = 52 - WEEKS_IN_FIRST_YEAR //35
        private val HOLIDAYS_LIST = HOLIDAYS_FIRST_YEAR + HOLIDAYS_SECOND_YEAR

        const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val AD_UNIT_ID = "ca-app-pub-4222736956813262/3450822903"
    }
}
