package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas // FIX: Added missing Canvas import
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.lifedots.graphics.GridDrawer
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    data class ThemeColors(val bg: Int, val card: Int, val accent: Int, val textPrimary: Int, val textSecondary: Int)

    private val themes = mapOf(
        "neon" to ThemeColors(Color.parseColor("#09090B"), Color.parseColor("#18181B"), Color.parseColor("#00F0FF"), Color.parseColor("#FFFFFF"), Color.parseColor("#A1A1AA")),
        "forest" to ThemeColors(Color.parseColor("#051C12"), Color.parseColor("#0F2E22"), Color.parseColor("#4ADE80"), Color.parseColor("#ECFDF5"), Color.parseColor("#6EE7B7")),
        "gold" to ThemeColors(Color.parseColor("#0F0F0F"), Color.parseColor("#1C1C1C"), Color.parseColor("#FFD700"), Color.parseColor("#FFFBEB"), Color.parseColor("#FDE68A")),
        "berry" to ThemeColors(Color.parseColor("#160B18"), Color.parseColor("#2D1B36"), Color.parseColor("#FB7185"), Color.parseColor("#FFF1F2"), Color.parseColor("#FDA4AF"))
    )

    private var currentTheme = themes["neon"]!!

    private lateinit var rootLayout: FrameLayout
    private lateinit var mainContentScroll: ScrollView
    private lateinit var titleText: TextView
    private lateinit var subTitleText: TextView
    private var allCards = mutableListOf<LinearLayout>()
    private var allHeaders = mutableListOf<TextView>()

    private lateinit var previewOverlay: FrameLayout
    private lateinit var previewImage: ImageView
    private lateinit var previewDimmer: View
    private lateinit var previewDots: PreviewView
    private lateinit var dimSlider: SeekBar
    private var tempSelectedUri: Uri? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var mScaleFactor = 1.0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var activePointerId = -1

    private lateinit var btnTabStandard: Button
    private lateinit var btnTabGoal: Button
    private lateinit var standardContainer: LinearLayout
    private lateinit var goalContainer: LinearLayout
    private lateinit var toggleContainer: LinearLayout
    private lateinit var btnRippleToggle: Button
    private lateinit var setWallpaperBtn: Button
    private lateinit var btnOpenDashboard: Button
    private lateinit var btnSaveGoal: Button

    private var selectedDateMillis: Long = 0

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) Toast.makeText(this, "Notifications Enabled!", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> openPreviewMode(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString("chosen_theme", "neon") ?: "neon"
        currentTheme = themes[savedThemeName]!!

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(currentTheme.bg)

        mainContentScroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(50, 80, 50, 80)
        }
        mainContentScroll.addView(mainLayout)
        rootLayout.addView(mainContentScroll)

        titleText = TextView(this).apply { text = "LifeDots"; textSize = 42f; setTextColor(currentTheme.textPrimary); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 20, 0, 10) }
        subTitleText = TextView(this).apply { text = "Visualize your time."; textSize = 14f; setTextColor(currentTheme.accent); gravity = Gravity.CENTER; setPadding(0, 0, 0, 60) }
        mainLayout.addView(titleText); mainLayout.addView(subTitleText)

        val bgCard = createCardLayout()
        bgCard.addView(createSectionHeader("WALLPAPER BACKGROUND"))
        val bgBtnLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pickBgBtn = createActionButton("Choose Image 🖼️", currentTheme.card)
        pickBgBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
        pickBgBtn.setOnClickListener { val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); pickImageLauncher.launch(intent) }
        val clearBgBtn = createActionButton("Reset to Color 🎨", currentTheme.card)
        clearBgBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10 }
        clearBgBtn.setOnClickListener {
            getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit()
                .putBoolean("use_custom_bg", false).putFloat("bg_scale", 1.0f).putFloat("bg_pos_x", 0f).putFloat("bg_pos_y", 0f).apply()
            val file = File(filesDir, "custom_bg.png"); if (file.exists()) file.delete()
            Toast.makeText(this@MainActivity, "Background Reset", Toast.LENGTH_SHORT).show()
            LifeDotsWidget.forceUpdateAll(this)
        }
        bgBtnLayout.addView(pickBgBtn); bgBtnLayout.addView(clearBgBtn); bgCard.addView(bgBtnLayout); mainLayout.addView(bgCard); mainLayout.addView(createSpacer(30))

        val themeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val themeLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        themeLayout.addView(createThemeButton("🌃 Neon", "neon")); themeLayout.addView(createThemeButton("🌿 Zen", "forest")); themeLayout.addView(createThemeButton("🏆 Gold", "gold")); themeLayout.addView(createThemeButton("🍬 Berry", "berry"))
        themeScroll.addView(themeLayout); mainLayout.addView(themeScroll); mainLayout.addView(createSpacer(40))

        toggleContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f; background = getRoundedDrawable(currentTheme.card, 50f); setPadding(10, 10, 10, 10) }
        btnTabStandard = createTabButton("Standard", true); btnTabGoal = createTabButton("Goal", false)
        btnTabStandard.setOnClickListener { switchTab(true) }; btnTabGoal.setOnClickListener { switchTab(false) }
        toggleContainer.addView(btnTabStandard); toggleContainer.addView(btnTabGoal); mainLayout.addView(toggleContainer); mainLayout.addView(createSpacer(40))

        val contentCard = createCardLayout()
        standardContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.VISIBLE }
        standardContainer.addView(createSectionHeader("TIMEFRAME MODE"))
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 4f }
        modeRow.addView(createOptionButton("Day", "day")); modeRow.addView(createOptionButton("Month", "month")); modeRow.addView(createOptionButton("Year", "year")); modeRow.addView(createOptionButton("Auto", "auto"))
        standardContainer.addView(modeRow); contentCard.addView(standardContainer)

        goalContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        goalContainer.addView(createSectionHeader("CUSTOM GOAL"))
        val goalInput = EditText(this).apply { hint = "Goal Name (e.g. Exam)"; setHintTextColor(Color.GRAY); setTextColor(currentTheme.textPrimary); textSize = 16f; background = getRoundedDrawable(Color.parseColor("#33000000"), 20f); setPadding(40, 30, 40, 30) }
        goalContainer.addView(goalInput); goalContainer.addView(createSpacer(20))
        val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dateBtn = createActionButton("Pick Date \uD83D\uDCC5", currentTheme.card); dateBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
        dateBtn.setOnClickListener { showDatePicker(dateBtn) }

        btnSaveGoal = createActionButton("Save", currentTheme.accent)
        btnSaveGoal.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE)
        btnSaveGoal.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10 }
        btnSaveGoal.setOnClickListener { if (selectedDateMillis > 0 && goalInput.text.isNotEmpty()) saveGoal(goalInput.text.toString(), selectedDateMillis) else Toast.makeText(this@MainActivity, "Enter name & date", Toast.LENGTH_SHORT).show() }

        dateRow.addView(dateBtn); dateRow.addView(btnSaveGoal); goalContainer.addView(dateRow); contentCard.addView(goalContainer); mainLayout.addView(contentCard)

        mainLayout.addView(createSpacer(30))
        val personaCard = createCardLayout()
        personaCard.addView(createSectionHeader("MOTIVATION PERSONA"))
        val personaScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val personaLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        personaLayout.addView(createPersonaButton("🎖️ Sergeant", "sergeant")); personaLayout.addView(createPersonaButton("🏛️ Stoic", "stoic")); personaLayout.addView(createPersonaButton("😎 Chill", "chill"))
        personaScroll.addView(personaLayout); personaCard.addView(personaScroll)

        // --- NEW FEATURE: CUSTOM QUOTES UI ---
        personaCard.addView(createSpacer(30))
        val btnCustomQuotes = createActionButton("ADD CUSTOM PHRASES ✍️", currentTheme.card)
        btnCustomQuotes.setOnClickListener { showCustomQuotesDialog() }
        personaCard.addView(btnCustomQuotes)
        mainLayout.addView(personaCard)

        mainLayout.addView(createSpacer(30))
        val focusCard = createCardLayout()
        focusCard.addView(createSectionHeader("FOCUS & ANALYSIS"))

        val focusDesc = TextView(this).apply { text = "Track usage, block distractions, and analyze your time."; textSize = 14f; setTextColor(currentTheme.textPrimary); setPadding(10, 0, 10, 30) }
        focusCard.addView(focusDesc)

        btnOpenDashboard = createActionButton("OPEN DASHBOARD \uD83D\uDCCA", currentTheme.accent)
        btnOpenDashboard.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE)
        btnOpenDashboard.setOnClickListener { attemptOpenDashboard() }

        focusCard.addView(btnOpenDashboard)
        mainLayout.addView(focusCard)

        mainLayout.addView(createSpacer(30))
        val shapeCard = createCardLayout()
        shapeCard.addView(createSectionHeader("DOT STYLE"))
        val shapeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val shapeLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shapeLayout.addView(createShapeChip("● Default", "circle")); shapeLayout.addView(createShapeChip("\uD83D\uDC8E Diamond", "diamond")); shapeLayout.addView(createShapeChip("🛡️ Tank", "tank")); shapeLayout.addView(createShapeChip("\uD83C\uDFAE Gamepad", "gamepad")); shapeLayout.addView(createShapeChip("\uD83C\uDF32 Tree", "tree")); shapeLayout.addView(createShapeChip("\uD83C\uDF4C Banana", "banana"))
        shapeScroll.addView(shapeLayout); shapeCard.addView(shapeScroll); mainLayout.addView(shapeCard)

        val styleCard = createCardLayout()
        styleCard.addView(createSectionHeader("TYPOGRAPHY"))
        val fontRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fontRow.addView(createFontButton("System", "system", null)); fontRow.addView(createFontButton("Marker", "patrick", R.font.patrick)); fontRow.addView(createFontButton("Console", "console", R.font.console)); fontRow.addView(createFontButton("Fancy", "lobster", R.font.lobster))
        styleCard.addView(fontRow); styleCard.addView(createSpacer(30)); styleCard.addView(createSectionHeader("INTERACTIVITY"))
        btnRippleToggle = createActionButton("Physics Ripples: ON", currentTheme.accent)
        btnRippleToggle.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE)
        btnRippleToggle.setOnClickListener { val p = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE); val newState = !p.getBoolean("ripple_enabled", true); p.edit().putBoolean("ripple_enabled", newState).apply(); updateRippleButtonState(newState) }
        updateRippleButtonState(prefs.getBoolean("ripple_enabled", true)); styleCard.addView(btnRippleToggle); mainLayout.addView(styleCard)
        mainLayout.addView(createSpacer(50)); setWallpaperBtn = createActionButton("APPLY LIVE WALLPAPER", currentTheme.accent); setWallpaperBtn.height = 180; setWallpaperBtn.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE); setWallpaperBtn.setOnClickListener { val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER); intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, YearProgressService::class.java)); startActivity(intent) }; mainLayout.addView(setWallpaperBtn); mainLayout.addView(createSpacer(50))

        setupPreviewOverlay()
        rootLayout.addView(previewOverlay)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        setContentView(rootLayout)
        LifeDotsWidget.forceUpdateAll(this)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("chosen_theme", "neon") ?: "neon"
        applyTheme(savedTheme)
    }

    // --- CUSTOM QUOTES DIALOG ---
    private fun showCustomQuotesDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val input = EditText(this).apply {
            hint = "e.g. I choose dopamine over success"
            setTextColor(Color.BLACK)
            setPadding(30, 30, 30, 30)
            background = getBorderDrawable(Color.GRAY)
        }
        container.addView(input)

        val existingQuotes = QuoteManager.getCustomQuotes(this)
        val listText = TextView(this).apply {
            text = if (existingQuotes.isEmpty()) "\nNo custom phrases yet. Using defaults." else "\nYour Custom Phrases:\n" + existingQuotes.joinToString("\n• ", prefix = "• ")
            setTextColor(Color.DKGRAY)
            textSize = 14f
            setPadding(0, 30, 0, 0)
        }
        container.addView(listText)

        AlertDialog.Builder(this)
            .setTitle("Add Custom Shame Phrase")
            .setView(container)
            .setPositiveButton("ADD") { _, _ ->
                val text = input.text.toString().trim()
                if(text.isNotEmpty()) {
                    QuoteManager.addCustomQuote(this, text)
                    Toast.makeText(this, "Phrase Saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("CLEAR ALL") { _,_ ->
                QuoteManager.clearCustomQuotes(this)
                Toast.makeText(this, "Cleared custom phrases.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    // --- ACCESSIBILITY PERMISSION LOGIC ---
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

    private fun attemptOpenDashboard() {
        val hasUsage = hasUsageStatsPermission()
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccess = isAccessibilityServiceEnabled(this, AppBlockerService::class.java)

        if (hasUsage && hasOverlay && hasAccess) {
            startActivity(Intent(this, FocusDashboardActivity::class.java))
        } else {
            showPermissionDialog(hasUsage, hasOverlay, hasAccess)
        }
    }

    private fun showPermissionDialog(hasUsage: Boolean, hasOverlay: Boolean, hasAccess: Boolean) {
        val msg = StringBuilder("To track usage and block apps, LifeDots needs access:\n")
        if (!hasUsage) msg.append("\n• Usage Access (To see time spent)")
        if (!hasOverlay) msg.append("\n• Overlay Access (To block apps)")
        if (!hasAccess) msg.append("\n• Accessibility (To detect apps instantly)")

        AlertDialog.Builder(this)
            .setTitle("Focus Mode Setup")
            .setMessage(msg.toString())
            .setPositiveButton("Grant Access") { _, _ ->
                if (!hasUsage) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                else if (!hasOverlay) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                else if (!hasAccess) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun setupPreviewOverlay() {
        previewOverlay = FrameLayout(this).apply { visibility = View.GONE; background = getRoundedDrawable(Color.BLACK, 0f); isClickable = true }
        previewImage = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
        previewImage.setOnTouchListener { _, event -> scaleGestureDetector.onTouchEvent(event); when (event.actionMasked) { MotionEvent.ACTION_DOWN -> { val pointerIndex = event.actionIndex; val x = event.getX(pointerIndex); val y = event.getY(pointerIndex); mLastTouchX = x; mLastTouchY = y; activePointerId = event.getPointerId(0) } MotionEvent.ACTION_MOVE -> { val pointerIndex = event.findPointerIndex(activePointerId); if (pointerIndex != -1) { val x = event.getX(pointerIndex); val y = event.getY(pointerIndex); val dx = x - mLastTouchX; val dy = y - mLastTouchY; mPosX += dx; mPosY += dy; previewImage.translationX = mPosX; previewImage.translationY = mPosY; mLastTouchX = x; mLastTouchY = y } } MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { activePointerId = -1 } MotionEvent.ACTION_POINTER_UP -> { val pointerIndex = event.actionIndex; val pointerId = event.getPointerId(pointerIndex); if (pointerId == activePointerId) { val newPointerIndex = if (pointerIndex == 0) 1 else 0; mLastTouchX = event.getX(newPointerIndex); mLastTouchY = event.getY(newPointerIndex); activePointerId = event.getPointerId(newPointerIndex) } } }; true }
        previewOverlay.addView(previewImage)
        previewDimmer = View(this).apply { setBackgroundColor(Color.BLACK); alpha = 0.4f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); isClickable = false; isFocusable = false }
        previewOverlay.addView(previewDimmer)
        previewDots = PreviewView(this).apply { layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); isClickable = false }
        previewOverlay.addView(previewDots)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#CC000000")); setPadding(50, 50, 50, 50); layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.BOTTOM } }
        val hintText = TextView(this).apply { text = "Pinch to Zoom • Drag to Move"; setTextColor(Color.LTGRAY); textSize = 12f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }
        controls.addView(hintText)
        val dimLabel = TextView(this).apply { text = "Brightness (Dimmer)"; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER }; controls.addView(dimLabel)
        dimSlider = SeekBar(this).apply { max = 255; progress = 100; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { previewDimmer.alpha = progress / 255f }; override fun onStartTrackingTouch(seekBar: SeekBar?) {}; override fun onStopTrackingTouch(seekBar: SeekBar?) {} }) }; controls.addView(dimSlider); controls.addView(createSpacer(30))
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cancelBtn = createActionButton("Cancel", Color.GRAY).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 20 }; setOnClickListener { previewOverlay.visibility = View.GONE; mainContentScroll.visibility = View.VISIBLE } }
        val saveBtn = createActionButton("Save", Color.parseColor("#4CAF50")).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { confirmSaveBackground() } }
        btnRow.addView(cancelBtn); btnRow.addView(saveBtn); controls.addView(btnRow); previewOverlay.addView(controls)
    }
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() { override fun onScale(detector: ScaleGestureDetector): Boolean { mScaleFactor *= detector.scaleFactor; mScaleFactor = max(1.0f, min(mScaleFactor, 5.0f)); previewImage.scaleX = mScaleFactor; previewImage.scaleY = mScaleFactor; return true } }
    private inner class PreviewView(context: Context) : View(context) { private val gridDrawer = GridDrawer(context); override fun onDraw(canvas: Canvas) { super.onDraw(canvas); gridDrawer.drawPreview(canvas) } }
    private fun openPreviewMode(uri: Uri) { tempSelectedUri = uri; try { val stream = contentResolver.openInputStream(uri); val bmp = BitmapFactory.decodeStream(stream); previewImage.setImageBitmap(bmp); mainContentScroll.visibility = View.GONE; previewOverlay.visibility = View.VISIBLE; val prefs = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE); val currentDim = prefs.getInt("bg_dim_amount", 100); dimSlider.progress = currentDim; previewDimmer.alpha = currentDim / 255f; mScaleFactor = prefs.getFloat("bg_scale", 1.0f); mPosX = prefs.getFloat("bg_pos_x", 0f); mPosY = prefs.getFloat("bg_pos_y", 0f); previewImage.scaleX = mScaleFactor; previewImage.scaleY = mScaleFactor; previewImage.translationX = mPosX; previewImage.translationY = mPosY; previewDots.invalidate() } catch (e: Exception) { Toast.makeText(this, "Failed to load preview", Toast.LENGTH_SHORT).show() } }
    private fun confirmSaveBackground() { val uri = tempSelectedUri ?: return; try { val inputStream = contentResolver.openInputStream(uri); val file = File(filesDir, "custom_bg.png"); val outputStream = FileOutputStream(file); inputStream?.copyTo(outputStream); inputStream?.close(); outputStream.close(); val dimLevel = dimSlider.progress; getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putBoolean("use_custom_bg", true).putInt("bg_dim_amount", dimLevel).putFloat("bg_scale", mScaleFactor).putFloat("bg_pos_x", mPosX).putFloat("bg_pos_y", mPosY).apply(); Toast.makeText(this, "Background Saved!", Toast.LENGTH_SHORT).show(); previewOverlay.visibility = View.GONE; mainContentScroll.visibility = View.VISIBLE; LifeDotsWidget.forceUpdateAll(this) } catch (e: Exception) { Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show() } }

    private fun applyTheme(themeKey: String) {
        val newTheme = themes[themeKey] ?: return
        currentTheme = newTheme
        getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_theme", themeKey).apply()

        rootLayout.setBackgroundColor(newTheme.bg)
        titleText.setTextColor(newTheme.textPrimary)
        subTitleText.setTextColor(newTheme.accent)
        allCards.forEach { it.background = getRoundedDrawable(newTheme.card, 30f) }
        allHeaders.forEach { it.setTextColor(newTheme.textSecondary) }
        toggleContainer.background = getRoundedDrawable(newTheme.card, 50f)
        switchTab(standardContainer.visibility == View.VISIBLE)

        btnRippleToggle.background = getRoundedDrawable(newTheme.accent, 25f)
        btnRippleToggle.setTextColor(if (isBright(newTheme.accent)) Color.BLACK else Color.WHITE)

        setWallpaperBtn.background = getRoundedDrawable(newTheme.accent, 25f)
        setWallpaperBtn.setTextColor(if (isBright(newTheme.accent)) Color.BLACK else Color.WHITE)

        btnOpenDashboard.background = getRoundedDrawable(newTheme.accent, 25f)
        btnOpenDashboard.setTextColor(if (isBright(newTheme.accent)) Color.BLACK else Color.WHITE)

        btnSaveGoal.background = getRoundedDrawable(newTheme.accent, 25f)
        btnSaveGoal.setTextColor(if (isBright(newTheme.accent)) Color.BLACK else Color.WHITE)

        Toast.makeText(this@MainActivity, "Theme Applied: $themeKey", Toast.LENGTH_SHORT).show()
        LifeDotsWidget.forceUpdateAll(this)
    }

    private fun isBright(color: Int): Boolean { return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 > 0.5 }
    private fun getRoundedDrawable(color: Int, radius: Float): GradientDrawable { return GradientDrawable().apply { setColor(color); cornerRadius = radius } }
    private fun getBorderDrawable(strokeColor: Int): GradientDrawable { return GradientDrawable().apply { setColor(Color.TRANSPARENT); setStroke(3, strokeColor); cornerRadius = 20f } }
    private fun createCardLayout(): LinearLayout { val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getRoundedDrawable(currentTheme.card, 30f); setPadding(40, 40, 40, 40) }; allCards.add(card); return card }
    private fun createSectionHeader(labelText: String): TextView { val tv = TextView(this).apply { text = labelText; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(currentTheme.textSecondary); setPadding(10, 0, 0, 25); letterSpacing = 0.1f }; allHeaders.add(tv); return tv }
    private fun createThemeButton(btnLabel: String, key: String): Button { return Button(this).apply { text = btnLabel; textSize = 14f; setTextColor(Color.WHITE); background = getRoundedDrawable(themes[key]!!.card, 40f); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 120).apply { marginEnd = 20 }; setOnClickListener { applyTheme(key) } } }
    private fun createTabButton(btnLabel: String, isActive: Boolean): Button { return Button(this).apply { text = btnLabel; background = if (isActive) getRoundedDrawable(currentTheme.accent, 40f) else null; setTextColor(if (isActive && isBright(currentTheme.accent)) Color.BLACK else if (isActive) Color.WHITE else Color.GRAY); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); stateListAnimator = null } }
    private fun createOptionButton(btnLabel: String, value: String): Button { return Button(this).apply { text = btnLabel; textSize = 13f; setTextColor(currentTheme.textPrimary); background = getRoundedDrawable(Color.TRANSPARENT, 0f); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_mode", value).apply(); Toast.makeText(this@MainActivity, "$btnLabel Selected", Toast.LENGTH_SHORT).show(); LifeDotsWidget.forceUpdateAll(this@MainActivity) } } }
    private fun createShapeChip(btnLabel: String, value: String): Button { return Button(this).apply { text = btnLabel; textSize = 14f; setTextColor(currentTheme.textPrimary); background = getBorderDrawable(Color.parseColor("#444444")); isAllCaps = false; setPadding(40, 0, 40, 0); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 120).apply { marginEnd = 20 }; setOnClickListener { getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_shape", value).apply(); Toast.makeText(this@MainActivity, "$btnLabel Selected", Toast.LENGTH_SHORT).show(); LifeDotsWidget.forceUpdateAll(this@MainActivity) } } }
    private fun createFontButton(btnLabel: String, value: String, fontResId: Int?): Button { return Button(this).apply { text = btnLabel.substring(0, 1); textSize = 18f; setTextColor(currentTheme.textPrimary); background = getRoundedDrawable(Color.parseColor("#33000000"), 100f); layoutParams = LinearLayout.LayoutParams(120, 120).apply { marginEnd = 20 }; if (fontResId != null) try { typeface = ResourcesCompat.getFont(this@MainActivity, fontResId) } catch (e: Exception) {}; setOnClickListener { getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_font", value).apply(); Toast.makeText(this@MainActivity, "$btnLabel Font", Toast.LENGTH_SHORT).show(); LifeDotsWidget.forceUpdateAll(this@MainActivity) } } }
    private fun createActionButton(btnLabel: String, color: Int): Button { return Button(this).apply { text = btnLabel; setTextColor(Color.WHITE); background = getRoundedDrawable(color, 25f); setTypeface(typeface, Typeface.BOLD) } }
    private fun updateRippleButtonState(isOn: Boolean) { val color = if (isOn) currentTheme.accent else Color.parseColor("#444444"); btnRippleToggle.text = if (isOn) "Physics Ripples: ON" else "Physics Ripples: OFF"; btnRippleToggle.background = getRoundedDrawable(color, 25f); btnRippleToggle.setTextColor(if (isOn && isBright(currentTheme.accent)) Color.BLACK else Color.WHITE) }
    private fun switchTab(showStandard: Boolean) { if (showStandard) { standardContainer.visibility = View.VISIBLE; goalContainer.visibility = View.GONE; btnTabStandard.background = getRoundedDrawable(currentTheme.accent, 40f); btnTabStandard.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE); btnTabGoal.background = null; btnTabGoal.setTextColor(Color.GRAY) } else { standardContainer.visibility = View.GONE; goalContainer.visibility = View.VISIBLE; btnTabStandard.background = null; btnTabStandard.setTextColor(Color.GRAY); btnTabGoal.background = getRoundedDrawable(currentTheme.accent, 40f); btnTabGoal.setTextColor(if (isBright(currentTheme.accent)) Color.BLACK else Color.WHITE) } }
    private fun showDatePicker(btn: Button) { val c = Calendar.getInstance(); DatePickerDialog(this, { _, y, m, d -> val cal = Calendar.getInstance(); cal.set(y, m, d); selectedDateMillis = cal.timeInMillis; btn.text = "$d/${m+1}/$y" }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }
    private fun saveGoal(name: String, endMillis: Long) { val start = System.currentTimeMillis(); val diff = endMillis - start; val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff); if (days <= 0) { Toast.makeText(this@MainActivity, "Future dates only!", Toast.LENGTH_SHORT).show(); return }; if (days > 366) { Toast.makeText(this@MainActivity, "Max 1 year.", Toast.LENGTH_LONG).show(); return }; getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_mode", "goal").putString("goal_name", name).putLong("goal_end", endMillis).putLong("goal_start", start).apply(); Toast.makeText(this@MainActivity, "GOAL SET: $name", Toast.LENGTH_LONG).show(); LifeDotsWidget.forceUpdateAll(this@MainActivity) }
    private fun createSpacer(height: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, height) }

    private fun createPersonaButton(btnLabel: String, value: String): Button { return Button(this).apply { text = btnLabel; textSize = 14f; setTextColor(currentTheme.textPrimary); background = getBorderDrawable(Color.parseColor("#444444")); isAllCaps = false; setPadding(40, 0, 40, 0); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 120).apply { marginEnd = 20 }; setOnClickListener { getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE).edit().putString("chosen_persona", value).apply(); Toast.makeText(this@MainActivity, "Persona: $btnLabel", Toast.LENGTH_SHORT).show(); NotificationHelper.sendNotification(this@MainActivity, value) } } }
}