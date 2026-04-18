package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.graphics.UsageGraphView
import com.example.lifedots.logic.AppCategoryHelper
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper
import java.util.Calendar
import java.util.Locale

class FocusDashboardActivity : AppCompatActivity() {

    private var activeCategoryFilter: String = "All"
    private var hasAnimatedTopStats = false

    private val sprintHandler = Handler(Looper.getMainLooper())
    private val sprintRunnable = object : Runnable {
        override fun run() {
            updateSprintUI()
            sprintHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_dashboard)

        setupCategoryChips()
        setupSprintButtons()

        findViewById<View>(R.id.cardSessions)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showSessionsInfoDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStartService()
        loadUsageData()
        updateCategoryChipUI()
        sprintHandler.post(sprintRunnable) // Start the visual tick down
    }

    override fun onPause() {
        super.onPause()
        sprintHandler.removeCallbacks(sprintRunnable)
    }

    // --- DEEP WORK SPRINT UI ---
    private fun setupSprintButtons() {
        findViewById<Button>(R.id.btnSprint25).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startSprint(25)
        }
        findViewById<Button>(R.id.btnSprint60).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startSprint(60)
        }
        findViewById<Button>(R.id.btnAbortSprint).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            // Aborting forces them into the Blocked Activity challenge
            val blockIntent = Intent(this, BlockedActivity::class.java)
            blockIntent.putExtra("BLOCKED_APP_NAME", "DEEP WORK ACTIVE")
            blockIntent.putExtra("BLOCKED_PACKAGE", packageName)
            startActivity(blockIntent)
        }
    }

    private fun startSprint(minutes: Int) {
        // 1. Safety Check: If Android 13+, ensure we have Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please enable notifications to run a Focus Sprint in the background.", Toast.LENGTH_LONG).show()
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }

        // 2. Start the Timer safely
        LimitManager.startFocusMode(this, minutes)
        val svcIntent = Intent(this, FocusSessionService::class.java)

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(svcIntent)
            } else {
                startService(svcIntent)
            }
            updateSprintUI()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start Deep Work. Please check permissions.", Toast.LENGTH_LONG).show()
            LimitManager.stopFocusMode(this) // Clean up if it failed
        }
    }

    private fun updateSprintUI() {
        val tvStatus = findViewById<TextView>(R.id.tvDeepWorkStatus)
        val layoutButtons = findViewById<LinearLayout>(R.id.layoutDeepWorkButtons)
        val btnAbort = findViewById<Button>(R.id.btnAbortSprint)

        if (LimitManager.isFocusModeActive(this)) {
            val remaining = LimitManager.getFocusModeEndTime(this) - System.currentTimeMillis()
            val mins = (remaining / 60_000).toInt()
            val secs = ((remaining % 60_000) / 1000).toInt()

            tvStatus.text = "Locked in. %02d:%02d remaining".format(mins, secs)
            tvStatus.setTextColor(Color.parseColor("#00F0FF"))
            layoutButtons.visibility = View.GONE
            btnAbort.visibility = View.VISIBLE
        } else {
            tvStatus.text = "Ready for a Deep Work sprint?"
            tvStatus.setTextColor(Color.WHITE)
            layoutButtons.visibility = View.VISIBLE
            btnAbort.visibility = View.GONE
        }
    }

    private fun setupCategoryChips() {
        val container = findViewById<LinearLayout>(R.id.containerCategoryChips)
        if (container == null) return
        container.removeAllViews()

        val categories = listOf("All", AppCategoryHelper.CAT_SOCIAL, AppCategoryHelper.CAT_VIDEO, AppCategoryHelper.CAT_GAMES, AppCategoryHelper.CAT_PRODUCTIVITY)

        for (cat in categories) {
            val btn = Button(this).apply {
                text = cat
                tag = cat
                textSize = 12f
                isAllCaps = false
                setPadding(40, 0, 40, 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 100).apply { marginEnd = 20 }

                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    activeCategoryFilter = cat
                    updateCategoryChipUI()
                    loadUsageData()
                }
            }
            container.addView(btn)
        }
    }

    private fun updateCategoryChipUI() {
        val container = findViewById<LinearLayout>(R.id.containerCategoryChips) ?: return
        for (i in 0 until container.childCount) {
            val btn = container.getChildAt(i) as? Button ?: continue
            val cat = btn.tag as? String ?: continue

            if (cat == activeCategoryFilter) {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
                btn.setTextColor(Color.BLACK)
            } else {
                val limit = if (cat != "All") LimitManager.getCategoryLimit(this, cat) else 0
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#18181B"))

                if (limit > 0) {
                    btn.setTextColor(Color.parseColor("#FF4444"))
                } else {
                    btn.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun addCategoryLimitCard(container: LinearLayout, category: String) {
        val limit = LimitManager.getCategoryLimit(this, category)
        val limitTextStr = if (limit > 0) "Collective Net Limit: ${UsageStatsHelper.getTimeString(limit * 60 * 1000L)}" else "No collective limit set"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = 30f
                setStroke(3, Color.parseColor("#00F0FF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 40 }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = "$category Setup"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = limitTextStr
            setTextColor(Color.parseColor("#A1A1AA"))
            textSize = 12f
            setPadding(0, 5, 0, 0)
        }

        textLayout.addView(title)
        textLayout.addView(subtitle)

        val btnSet = Button(this).apply {
            text = if (limit > 0) "EDIT" else "SET LIMIT"
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 100)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                showCategoryDialPicker(category, limit)
            }
        }

        card.addView(textLayout)
        card.addView(btnSet)
        container.addView(card)
    }

    private fun showCategoryDialPicker(categoryName: String, currentLimit: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_limit_picker, null)
        val pickerHours = dialogView.findViewById<NumberPicker>(R.id.pickerHours).apply { minValue = 0; maxValue = 12; value = currentLimit / 60 }
        val pickerMinutes = dialogView.findViewById<NumberPicker>(R.id.pickerMinutes).apply { minValue = 0; maxValue = 59; value = currentLimit % 60 }

        val alert = AlertDialog.Builder(this).setView(dialogView).create()
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btnSaveLimit).setOnClickListener {
            val total = (pickerHours.value * 60) + pickerMinutes.value
            if (total == 0) LimitManager.removeCategoryLimit(this, categoryName)
            else LimitManager.saveCategoryLimit(this, categoryName, total)

            updateCategoryChipUI()
            loadUsageData()
            alert.dismiss()
        }
        alert.show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        if (enabledServices != null) {
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals("${context.packageName}/${service.name}", ignoreCase = true) ||
                    componentName.equals("${context.packageName}/${service.canonicalName}", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkPermissionsAndStartService() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)

        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun loadUsageData() {
        val allUsageList = UsageStatsHelper.getTodayUsage(this)

        val filteredUsageList = if (activeCategoryFilter == "All") {
            allUsageList
        } else {
            allUsageList.filter { it.category == activeCategoryFilter }
        }

        var totalTimeMillis = 0L
        allUsageList.forEach { totalTimeMillis += it.timeInForeground }

        val sessions = UsageStatsHelper.getAppLaunchCount(this)

        val txtTotalTime = findViewById<TextView>(R.id.txtTotalTime)
        val txtLifeDrain = findViewById<TextView>(R.id.txtLifeDrainDesc)
        val txtSessionCount = findViewById<TextView>(R.id.txtSessionCount)
        val txtGrade = findViewById<TextView>(R.id.txtFocusGrade)

        val wakingHoursMillis = 16 * 60 * 60 * 1000L
        val percent = ((totalTimeMillis.toFloat() / wakingHoursMillis.toFloat()) * 100).coerceAtMost(100f)
        val hours = totalTimeMillis / 1000 / 3600
        val (grade, color) = calculateGrade(hours, sessions)

        txtGrade.text = grade
        txtGrade.setTextColor(color)

        if (!hasAnimatedTopStats) {
            animateTotalTime(txtTotalTime, totalTimeMillis)
            animatePercentage(txtLifeDrain, percent)
            animateIntCount(txtSessionCount, sessions)
            popInView(txtGrade)
            hasAnimatedTopStats = true
        } else {
            txtTotalTime.text = UsageStatsHelper.getTimeString(totalTimeMillis)
            txtLifeDrain.text = String.format(Locale.US, "Used %.0f%% of waking hours", percent)
            txtSessionCount.text = sessions.toString()
            txtGrade.alpha = 1f
            txtGrade.scaleX = 1f
            txtGrade.scaleY = 1f
        }

        val container = findViewById<LinearLayout>(R.id.containerAppList)
        container.removeAllViews()

        if (activeCategoryFilter != "All") {
            addCategoryLimitCard(container, activeCategoryFilter)
        }

        val maxUsage = filteredUsageList.firstOrNull()?.timeInForeground ?: 1L

        for (app in filteredUsageList) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_app_usage, container, false)

            val imgIcon = view.findViewById<ImageView>(R.id.imgAppIcon)
            val txtName = view.findViewById<TextView>(R.id.txtAppName)
            val txtTime = view.findViewById<TextView>(R.id.txtAppTime)
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBarUsage)
            val btnLimit = view.findViewById<Button>(R.id.btnBlockApp)
            val txtCategory = view.findViewById<TextView>(R.id.txtAppCategory)

            imgIcon.setImageDrawable(app.icon)
            txtName.text = app.appName
            txtTime.text = UsageStatsHelper.getTimeString(app.timeInForeground)

            if (txtCategory != null) {
                txtCategory.text = app.category
                txtCategory.setTextColor(Color.GRAY)
            }

            val appLimit = LimitManager.getLimit(this, app.packageName)
            val catLimit = LimitManager.getCategoryLimit(this, app.category)

            val isRestrictedByCategory = catLimit > 0
            val isRestrictedByApp = appLimit > 0

            if (isRestrictedByCategory || isRestrictedByApp) {
                btnLimit.backgroundTintList = ColorStateList.valueOf(Color.RED)
                if (isRestrictedByApp) btnLimit.text = UsageStatsHelper.getTimeString(appLimit * 60 * 1000L)
                else btnLimit.text = "CAT LIMIT"

                val appUsageMins = app.timeInForeground / 1000 / 60
                if (isRestrictedByApp && appUsageMins >= appLimit) {
                    txtName.setTextColor(Color.RED)
                    txtTime.setTextColor(Color.RED)
                }
            } else {
                btnLimit.text = "LIMIT"
                btnLimit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
                txtName.setTextColor(Color.WHITE)
                txtTime.setTextColor(Color.parseColor("#00F0FF"))
            }

            val progress = (app.timeInForeground.toFloat() / maxUsage.toFloat()) * 100
            progressBar.progress = 0
            animateProgressBar(progressBar, progress.toInt())

            btnLimit.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                showDialPicker(app.appName, app.packageName, appLimit)
            }

            view.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                showAppStatsDialog(app.appName, app.packageName, app.icon, app.timeInForeground, app.category)
            }

            container.addView(view)
        }
    }

    private fun animateTotalTime(textView: TextView, targetMillis: Long) {
        val animator = ValueAnimator.ofFloat(0f, targetMillis.toFloat())
        animator.duration = 2500L
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val v = (animation.animatedValue as Float).toLong()
            textView.text = UsageStatsHelper.getTimeString(v)
        }
        animator.start()
    }

    private fun animatePercentage(textView: TextView, targetPercent: Float) {
        val animator = ValueAnimator.ofFloat(0f, targetPercent)
        animator.duration = 2500L
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val v = animation.animatedValue as Float
            textView.text = String.format(Locale.US, "Used %.0f%% of waking hours", v)
        }
        animator.start()
    }

    private fun animateIntCount(textView: TextView, targetValue: Int) {
        val animator = ValueAnimator.ofInt(0, targetValue)
        animator.duration = 2500L
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun animateProgressBar(progressBar: ProgressBar, targetProgress: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animator.duration = 1500L
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private fun popInView(view: View) {
        view.alpha = 0f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200L)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    private fun calculateGrade(hours: Long, sessions: Int): Pair<String, Int> {
        return when {
            hours < 2 -> Pair("A", Color.parseColor("#4ADE80"))
            hours < 4 -> Pair("B", Color.parseColor("#A3E635"))
            hours < 6 -> Pair("C", Color.parseColor("#FACC15"))
            hours < 8 -> Pair("D", Color.parseColor("#FB923C"))
            else -> Pair("F", Color.parseColor("#F87171"))
        }
    }

    private fun showSessionsInfoDialog() {
        var localDialogRef: AlertDialog? = null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = 50f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
        }

        val titleView = TextView(this).apply {
            text = "App Hopping"
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 30 }
        }

        val descView = TextView(this).apply {
            text = "This number measures Context Switching (also known as 'App Hopping').\n\nEvery single time you unlock your phone, pull down your notifications, or switch from one app to another, it counts as 1 session.\n\nA high number means your attention is heavily fragmented and you are constantly bouncing between distractions."
            textSize = 14f
            setTextColor(Color.parseColor("#A1A1AA"))
            setLineSpacing(0f, 1.3f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 40 }
        }

        val btnGotIt = Button(this).apply {
            text = "GOT IT"
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                localDialogRef?.dismiss()
            }
        }

        container.addView(titleView)
        container.addView(descView)
        container.addView(btnGotIt)

        localDialogRef = AlertDialog.Builder(this)
            .setView(container)
            .create()

        localDialogRef.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        localDialogRef.show()
    }

    private fun showAppStatsDialog(appName: String, packageName: String, icon: Drawable?, timeInForeground: Long, currentCategory: String) {
        var localDialogRef: AlertDialog? = null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 50)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = 50f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 50
            }
        }

        val imgView = ImageView(this).apply {
            setImageDrawable(icon)
            layoutParams = LinearLayout.LayoutParams(110, 110).apply { marginEnd = 30 }
        }

        val titleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(this).apply {
            text = appName
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
        }

        val catView = TextView(this).apply {
            text = currentCategory
            textSize = 13f
            setTextColor(Color.parseColor("#A1A1AA"))
        }

        val btnEditCat = TextView(this).apply {
            text = "✎ EDIT CATEGORY"
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00F0FF"))
            setPadding(20, 8, 20, 8)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A00F0FF"))
                setStroke(2, Color.parseColor("#00F0FF"))
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = 20 }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                localDialogRef?.dismiss()
                showCategoryOverrideDialog(appName, packageName, currentCategory)
            }
        }

        catRow.addView(catView)
        catRow.addView(btnEditCat)

        titleLayout.addView(titleView)
        titleLayout.addView(catRow)

        headerRow.addView(imgView)
        headerRow.addView(titleLayout)
        container.addView(headerRow)

        val statsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#09090B"))
                cornerRadius = 30f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 50 }
        }

        val timeLabel = TextView(this).apply {
            text = "Time Today"
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        val timeView = TextView(this).apply {
            text = UsageStatsHelper.getTimeString(timeInForeground)
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val events = usm.queryEvents(midnight, System.currentTimeMillis())
        var specificAppSessions = 0
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName && event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) specificAppSessions++
        }

        val sessLabel = TextView(this).apply {
            text = "App Opened"
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        val sessionView = TextView(this).apply {
            text = "$specificAppSessions times"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        statsBox.addView(timeLabel)
        statsBox.addView(timeView)
        statsBox.addView(sessLabel)
        statsBox.addView(sessionView)
        container.addView(statsBox)

        val graphLabel = TextView(this).apply {
            text = "ACTIVITY (PAST 12h)"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.GRAY)
            isAllCaps = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 15 }
        }
        container.addView(graphLabel)

        val graphView = UsageGraphView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)
        }
        val hourlyData = UsageStatsHelper.getAppUsageHourly(this, packageName)
        graphView.setData(hourlyData, animate = true)
        container.addView(graphView)

        localDialogRef = AlertDialog.Builder(this)
            .setView(container)
            .create()

        localDialogRef.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        localDialogRef.show()
    }

    private fun showCategoryOverrideDialog(appName: String, packageName: String, currentCat: String) {
        val categories = AppCategoryHelper.getAllCategories()
        val currentIndex = categories.indexOf(currentCat).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Move $appName")
            .setSingleChoiceItems(categories, currentIndex) { dialog, which ->
                val chosenCat = categories[which]
                LimitManager.saveCustomCategory(this, packageName, chosenCat)
                Toast.makeText(this, "Moved to $chosenCat", Toast.LENGTH_SHORT).show()
                loadUsageData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDialPicker(appName: String, packageName: String, currentLimit: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_limit_picker, null)
        val pickerHours = dialogView.findViewById<NumberPicker>(R.id.pickerHours).apply { minValue = 0; maxValue = 12; value = currentLimit / 60 }
        val pickerMinutes = dialogView.findViewById<NumberPicker>(R.id.pickerMinutes).apply { minValue = 0; maxValue = 59; value = currentLimit % 60 }

        val alert = AlertDialog.Builder(this).setView(dialogView).create()
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btnSaveLimit).setOnClickListener {
            val total = (pickerHours.value * 60) + pickerMinutes.value
            if (total == 0) LimitManager.removeLimit(this, packageName)
            else LimitManager.saveLimit(this, packageName, total)
            loadUsageData()
            alert.dismiss()
        }
        alert.show()
    }
}