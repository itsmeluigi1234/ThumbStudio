package com.thumbstudio.app

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var preview: ImageView
    private lateinit var titleInput: EditText
    private lateinit var subtitleInput: EditText

    private var sceneBitmap: Bitmap? = null
    private var characterBitmap: Bitmap? = null
    private var backgroundColor = Color.rgb(35, 22, 68)
    private var titleColor = Color.WHITE
    private var outlineColor = Color.BLACK
    private var titleSize = 92f
    private var titleX = 0.50f
    private var titleY = 0.82f
    private var subtitleX = 0.50f
    private var subtitleY = 0.94f
    private var characterX = 0.73f
    private var characterY = 0.61f
    private var characterScale = 0.52f
    private var selectedLayer = Layer.TITLE
    private var pickerTarget = PickerTarget.SCENE
    private var touchX = 0f
    private var touchY = 0f
    private var initialX = 0f
    private var initialY = 0f

    private enum class Layer { TITLE, SUBTITLE, CHARACTER }
    private enum class PickerTarget { SCENE, CHARACTER }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                        ?: throw IllegalArgumentException("Unsupported image")
                    if (pickerTarget == PickerTarget.SCENE) {
                        sceneBitmap?.takeIf { it !== bitmap }?.recycle()
                        sceneBitmap = bitmap
                    } else {
                        characterBitmap?.takeIf { it !== bitmap }?.recycle()
                        characterBitmap = bitmap
                    }
                }
                renderPreview()
            } catch (e: Exception) {
                toast("Couldn't load image: ${e.message ?: "unknown error"}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 25)
        window.navigationBarColor = Color.rgb(16, 16, 25)

        val scroll = ScrollView(this).apply {
            fillViewport = true
            setBackgroundColor(Color.rgb(16, 16, 25))
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        scroll.addView(page)

        page.addView(textView("ThumbStudio", 28f, Color.rgb(255, 205, 45), true, Gravity.CENTER))
        page.addView(textView("ROBLOX GAME THUMBNAIL CREATOR", 12f, Color.LTGRAY, true, Gravity.CENTER).apply {
            setPadding(0, 0, 0, dp(12))
        })

        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            setBackgroundColor(Color.rgb(40, 25, 65))
            contentDescription = "Thumbnail canvas. Drag the selected layer to reposition it."
        }
        page.addView(preview, LinearLayout.LayoutParams(-1, dp(205)).apply {
            bottomMargin = dp(10)
        })
        preview.setOnTouchListener { _, event -> handleCanvasTouch(event) }

        page.addView(section("1 · CHOOSE A ROBLOX STYLE"))
        page.addView(buttonRow(listOf("Obby", "Simulator", "Horror", "Action")) { preset ->
            applyPreset(preset)
        })

        page.addView(section("2 · BUILD THE GAME SCENE"))
        page.addView(buttonRow(listOf("Import Scene", "Import Character")) { choice ->
            pickerTarget = if (choice == "Import Scene") PickerTarget.SCENE else PickerTarget.CHARACTER
            imagePicker.launch("image/*")
        })
        page.addView(buttonRow(listOf("Remove Scene", "Remove Character")) { choice ->
            if (choice == "Remove Scene") {
                sceneBitmap?.recycle()
                sceneBitmap = null
            } else {
                characterBitmap?.recycle()
                characterBitmap = null
            }
            renderPreview()
        })
        page.addView(textView(
            "Tip: use a Roblox screenshot for the scene and a character PNG with a transparent background for the foreground character.",
            12f, Color.LTGRAY, false, Gravity.START
        ))

        page.addView(section("3 · ADD YOUR TEXT"))
        titleInput = editField("MAIN TITLE — e.g. I SURVIVED 100 DAYS")
        subtitleInput = editField("Subtitle — optional")
        page.addView(titleInput)
        page.addView(subtitleInput)
        titleInput.addTextChangedListener(SimpleWatcher { renderPreview() })
        subtitleInput.addTextChangedListener(SimpleWatcher { renderPreview() })

        page.addView(buttonRow(listOf("Move Title", "Move Subtitle", "Move Character")) { choice ->
            selectedLayer = when (choice) {
                "Move Subtitle" -> Layer.SUBTITLE
                "Move Character" -> Layer.CHARACTER
                else -> Layer.TITLE
            }
            toast("Selected ${selectedLayer.name.lowercase()}. Drag it on the canvas.")
        })
        page.addView(buttonRow(listOf("Text −", "Text +", "Character −", "Character +")) { choice ->
            when (choice) {
                "Text −" -> titleSize = (titleSize - 8f).coerceIn(28f, 170f)
                "Text +" -> titleSize = (titleSize + 8f).coerceIn(28f, 170f)
                "Character −" -> characterScale = (characterScale - 0.05f).coerceIn(0.15f, 0.9f)
                "Character +" -> characterScale = (characterScale + 0.05f).coerceIn(0.15f, 0.9f)
            }
            renderPreview()
        })

        page.addView(section("4 · COLORS"))
        page.addView(buttonRow(listOf("Purple BG", "Blue BG", "Green BG", "Red BG")) { choice ->
            sceneBitmap = null
            backgroundColor = when (choice) {
                "Purple BG" -> Color.rgb(85, 35, 160)
                "Blue BG" -> Color.rgb(25, 110, 210)
                "Green BG" -> Color.rgb(30, 140, 70)
                else -> Color.rgb(190, 35, 48)
            }
            renderPreview()
        })
        page.addView(buttonRow(listOf("White Text", "Yellow Text", "Cyan Text", "Pink Text")) { choice ->
            titleColor = when (choice) {
                "Yellow Text" -> Color.YELLOW
                "Cyan Text" -> Color.CYAN
                "Pink Text" -> Color.rgb(255, 90, 190)
                else -> Color.WHITE
            }
            renderPreview()
        })
        page.addView(buttonRow(listOf("Black Outline", "Purple Outline", "Red Outline")) { choice ->
            outlineColor = when (choice) {
                "Purple Outline" -> Color.rgb(140, 40, 255)
                "Red Outline" -> Color.rgb(235, 35, 45)
                else -> Color.BLACK
            }
            renderPreview()
        })

        page.addView(section("5 · EXPORT"))
        page.addView(buttonRow(listOf("Export 1280 × 720 PNG")) { exportPng() })
        page.addView(textView(
            "Exports are saved in Pictures/ThumbStudio. This editor combines images and text; it does not automatically generate 3D Roblox characters.",
            12f, Color.LTGRAY, false, Gravity.START
        ).apply { setPadding(0, dp(8), 0, dp(4)) })

        setContentView(scroll)
        applyPreset("Obby")
    }

    private fun applyPreset(name: String) {
        when (name) {
            "Obby" -> {
                backgroundColor = Color.rgb(28, 155, 218)
                titleInput.setText("WORLD'S HARDEST OBBY!")
                subtitleInput.setText("CAN YOU BEAT IT?")
                titleColor = Color.YELLOW
                outlineColor = Color.rgb(90, 45, 15)
            }
            "Simulator" -> {
                backgroundColor = Color.rgb(35, 145, 72)
                titleInput.setText("I GOT OP!")
                subtitleInput.setText("NEW UPDATE")
                titleColor = Color.YELLOW
                outlineColor = Color.rgb(25, 55, 20)
            }
            "Horror" -> {
                backgroundColor = Color.rgb(28, 10, 42)
                titleInput.setText("DON'T LOOK BACK")
                subtitleInput.setText("IT'S RIGHT BEHIND YOU")
                titleColor = Color.rgb(255, 65, 75)
                outlineColor = Color.BLACK
            }
            else -> {
                backgroundColor = Color.rgb(35, 60, 165)
                titleInput.setText("ULTIMATE BATTLE")
                subtitleInput.setText("WHO WILL WIN?")
                titleColor = Color.WHITE
                outlineColor = Color.rgb(120, 35, 210)
            }
        }
        sceneBitmap = null
        titleSize = 92f
        titleX = 0.5f
        titleY = 0.82f
        subtitleX = 0.5f
        subtitleY = 0.94f
        characterX = 0.73f
        characterY = 0.61f
        renderPreview()
    }

    private fun handleCanvasTouch(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = e.x
                touchY = e.y
                when (selectedLayer) {
                    Layer.TITLE -> { initialX = titleX; initialY = titleY }
                    Layer.SUBTITLE -> { initialX = subtitleX; initialY = subtitleY }
                    Layer.CHARACTER -> { initialX = characterX; initialY = characterY }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (preview.width <= 0 || preview.height <= 0) return true
                val x = (initialX + (e.x - touchX) / preview.width).coerceIn(0.04f, 0.96f)
                val y = (initialY + (e.y - touchY) / preview.height).coerceIn(0.08f, 0.98f)
                when (selectedLayer) {
                    Layer.TITLE -> { titleX = x; titleY = y }
                    Layer.SUBTITLE -> { subtitleX = x; subtitleY = y }
                    Layer.CHARACTER -> { characterX = x; characterY = y }
                }
                renderPreview()
                return true
            }
        }
        return true
    }

    private fun renderPreview() {
        if (!::preview.isInitialized || !::titleInput.isInitialized) return
        val bitmap = renderThumbnail(640, 360)
        val old = (preview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        preview.setImageBitmap(bitmap)
        if (old != null && old !== bitmap && !old.isRecycled) old.recycle()
    }

    private fun renderThumbnail(width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        sceneBitmap?.let { drawCover(canvas, it, Rect(0, 0, width, height)) }
        drawBackgroundEffects(canvas, width, height)
        characterBitmap?.let { char ->
            val maxW = width * characterScale
            val ratio = min(maxW / char.width, height * 0.92f / char.height)
            val w = char.width * ratio
            val h = char.height * ratio
            val dst = RectF(characterX * width - w / 2f, characterY * height - h / 2f,
                characterX * width + w / 2f, characterY * height + h / 2f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(char, null, dst, paint)
        }
        drawStyledText(canvas, titleInput.text.toString(), titleX * width, titleY * height,
            titleSize * width / 1280f, true)
        drawStyledText(canvas, subtitleInput.text.toString(), subtitleX * width, subtitleY * height,
            titleSize * 0.38f * width / 1280f, false)
        return output
    }

    private fun drawCover(canvas: Canvas, bitmap: Bitmap, dst: Rect) {
        val scale = max(dst.width().toFloat() / bitmap.width, dst.height().toFloat() / bitmap.height)
        val w = dst.width() / scale
        val h = dst.height() / scale
        val src = Rect(
            ((bitmap.width - w) / 2f).toInt().coerceAtLeast(0),
            ((bitmap.height - h) / 2f).toInt().coerceAtLeast(0),
            ((bitmap.width + w) / 2f).toInt().coerceAtMost(bitmap.width),
            ((bitmap.height + h) / 2f).toInt().coerceAtMost(bitmap.height)
        )
        canvas.drawBitmap(bitmap, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawBackgroundEffects(canvas: Canvas, width: Int, height: Int) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(width * 0.72f, height * 0.43f, width * 0.75f,
                intArrayOf(Color.argb(65, 210, 70, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
        val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, height * .55f, 0f, height.toFloat(),
                Color.TRANSPARENT, Color.argb(190, 0, 0, 0), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
    }

    private fun drawStyledText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, main: Boolean) {
        if (value.isBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = size.coerceAtLeast(10f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setShadowLayer(if (main) 5f else 3f, 2f, 3f, Color.BLACK)
        }
        val outline = Paint(paint).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = if (main) max(2f, size * .09f) else max(1.5f, size * .08f)
            strokeJoin = Paint.Join.ROUND
            clearShadowLayer()
        }
        val maxWidth = canvas.width * .9f
        val scale = if (paint.measureText(value) > maxWidth) maxWidth / paint.measureText(value) else 1f
        canvas.save()
        canvas.scale(scale, scale, x, y)
        canvas.drawText(value, x, y, outline)
        canvas.drawText(value, x, y, paint)
        canvas.restore()
    }

    private fun exportPng() {
        val bitmap = renderThumbnail(1280, 720)
        var uri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "ThumbStudio_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ThumbStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create image file")
            contentResolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("PNG encoding failed")
                }
            } ?: throw IllegalStateException("Could not open image file")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
            }
            toast("Saved PNG to Pictures/ThumbStudio")
        } catch (e: Exception) {
            uri?.let { contentResolver.delete(it, null, null) }
            toast("Export failed: ${e.message ?: "unknown error"}")
        } finally {
            bitmap.recycle()
        }
    }

    private fun section(value: String) = textView(value, 12f, Color.rgb(200, 200, 215), true, Gravity.START).apply {
        setPadding(dp(2), dp(12), dp(2), dp(5))
    }

    private fun textView(value: String, size: Float, color: Int, bold: Boolean, gravity: Int) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            this.gravity = gravity
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun editField(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 14f
        setSingleLine(true)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.GRAY)
        setPadding(dp(12), dp(4), dp(12), dp(4))
        setBackgroundColor(Color.rgb(38, 38, 52))
        layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(6)
        }
    }

    private fun buttonRow(names: List<String>, action: (String) -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            names.forEach { name ->
                val button = Button(this@MainActivity).apply {
                    text = name
                    textSize = 10f
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(58, 53, 82)))
                    setPadding(dp(2), 0, dp(2), 0)
                    minHeight = dp(42)
                    setOnClickListener { action(name) }
                }
                addView(button, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
        }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class SimpleWatcher(private val changed: () -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
