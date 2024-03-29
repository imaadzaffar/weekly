package com.zafaris.whichweek

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.datepicker.MaterialDatePicker
import com.zafaris.whichweek.databinding.ActivityMainBinding
import io.github.muddz.styleabletoast.StyleableToast
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import org.threeten.extra.YearWeek
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var prefs: SharedPreferences

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
    private var currentYearISO = 0
    private var currentWeekISO = 0
    private var weeksInFirstYear: Int = 0

    private var weekList: ArrayList<Int>? = null
    private var weekFormat = 0
    private var weekFormatList: ArrayList<String>? = arrayListOf("A", "B")
    private var currentWeekType = 0
    private var nextWeekType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        hideStatusBar()
        animateBackground()

        getDateInfo()

        prefs = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

        // Remove old prefs
        prefs.edit().remove("firstYear").apply()
        prefs.edit().remove("weekList").apply()

        when {
            prefs.getBoolean("firstTime", true) -> {
                prefs.edit().putBoolean("firstTime", false).apply()
                showStartDialog()
            }
            prefs.getInt("firstYear", 0) == FIRST_YEAR -> {

            }
        }
        generateWeeksList()

        binding.changeFormatSwitch.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                false -> changeWeekFormat(1)
                true -> changeWeekFormat(2)
            }
        }

        // Retrieves week format from device (A/B or 1/2)
        weekFormat = prefs.getInt("weekFormat", 1)
        // Toggle switch if week format is 1/2
        if (weekFormat == 2) {
            binding.changeFormatSwitch.isChecked = true
        }

        val weeksText = generateWeeksText()
        binding.currentWeekTv.text = weeksText[0]
        binding.nextWeekTv.text = weeksText[1]

        //TODO: Uncomment this
        setupBannerAd()
    }

    private fun showStartDialog() {
        AlertDialog.Builder(this)
            .setTitle("Info")
            .setMessage(
                "This app was made according to the King Edward VI Birmingham, UK school timetables. " +
                        "\n\nIf you do not attend one of these schools, the week types and holidays shown might not be correct for you."
            )
            .setPositiveButton("I understand") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun generateWeeksList() {
        weekList = ArrayList()

        // School year starts on week A
        var tmpWeekType = 1
        var tmpYear: Int

        // Log.i("HOLIDAYS_LIST", HOLIDAYS_LIST.toString())

        // Assigning values to each week of the year
        for (i in 0..weeksInFirstYear) {
            var tmpWeekISO: Int
            if (i < SCHOOL_WEEKS_IN_FIRST_YEAR) {
                tmpWeekISO = FIRST_WEEK_ISO + i
                tmpYear = FIRST_YEAR
            } else {
                tmpWeekISO = (i - SCHOOL_WEEKS_IN_FIRST_YEAR) + 1
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

            // Log.i("Weeks List", "Year: $tmpYear, Week: $tmpWeekISO, Week Type: ${weekList!![i]}")
        }
    }

    private fun generateWeeksText(): Array<String?> {
        val weeksText = arrayOfNulls<String>(2)

        val week1 = weekFormatList!![0]
        val week2 = weekFormatList!![1]

        if (currentYearISO == FIRST_YEAR) {
            // Checks if it is the last week of the year
            if (currentWeekISO == SCHOOL_WEEKS_IN_FIRST_YEAR) {
                currentWeekType = weekList!![SCHOOL_WEEKS_IN_FIRST_YEAR - 1]
                nextWeekType = weekList!![(SCHOOL_WEEKS_IN_FIRST_YEAR - 1) + 1]
            } else {
                currentWeekType = weekList!![currentWeekISO - FIRST_WEEK_ISO]
                nextWeekType = weekList!![(currentWeekISO - FIRST_WEEK_ISO) + 1]
            }
        } else {
            currentWeekType = weekList!![currentWeekISO + SCHOOL_WEEKS_IN_FIRST_YEAR - 1]
            nextWeekType = weekList!![(currentWeekISO + SCHOOL_WEEKS_IN_FIRST_YEAR - 1) + 1]
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
        binding.fullDateTv.text = fullDate

        // Week of year ISO
        val zoneId = ZoneId.of("Europe/London")
        today = LocalDate.now(zoneId)
        currentYearISO = YearWeek.from(today).year
        currentWeekISO = YearWeek.from(today).week
        weeksInFirstYear = if (YearWeek.from(today).is53WeekYear) 53 else 52
    }
    
    private fun getWeekType(date: LocalDate): Int {
        val yearISO = YearWeek.from(date).year
        val weekISO = YearWeek.from(date).week
        return if (yearISO == FIRST_YEAR) {
            // If last week of the year
            if (weekISO == SCHOOL_WEEKS_IN_FIRST_YEAR) {
                weekList!![SCHOOL_WEEKS_IN_FIRST_YEAR - 1]
            } else {
                weekList!![weekISO - FIRST_WEEK_ISO]
            }
        } else {
            weekList!![weekISO + SCHOOL_WEEKS_IN_FIRST_YEAR - 1]
        }
    }

    private fun changeWeekFormat(weekFormat: Int) {
        weekFormatList = if (weekFormat == 1) {
            arrayListOf("A", "B")
        } else {
            arrayListOf("1", "2")
        }

        prefs.edit().putInt("weekFormat", weekFormat).apply()
        val weeksText = generateWeeksText()
        binding.currentWeekTv.text = weeksText[0]
        binding.nextWeekTv.text = weeksText[1]
    }
    
    fun tapScreenAnimation(view: View?) {
        val centreLayout = findViewById<ConstraintLayout>(R.id.centreLayout)
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce_animation)
        centreLayout.startAnimation(bounce)

        // Checks if there aren't already any konfetti animations going on
        if (binding.viewKonfetti.getActiveSystems().size == 0) {
            binding.viewKonfetti.build()
                .addColors(Color.WHITE)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.Square, Shape.Circle)
                .addSizes(Size(12))
                .setPosition(-50f, binding.viewKonfetti.width + 50f, -50f, -50f)
                .streamFor(300, 2000L)
        }
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
        val weeksText = generateWeeksText()
        var message = weeksText[0] + "\n" + weeksText[1]
        message +=  "\n\nNever forget which week it is again with the Week.ly app! " +
                "Download now: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID

        val shareIntent = Intent()
        shareIntent.type = "text/plain"
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
        startActivity(Intent.createChooser(shareIntent, "Share with: "))
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
        MobileAds.initialize(this)
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
    }
}
