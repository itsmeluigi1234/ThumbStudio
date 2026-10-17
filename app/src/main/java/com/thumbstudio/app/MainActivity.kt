
package com.thumbstudio.app

import android.app.Activity
import android.os.Bundle
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : Activity() {

    private lateinit var preview: ImageView
    private lateinit var titleInput: EditText
    private lateinit var subtitleInput: EditText

    private var backgroundBitmap: Bitmap? = null
    private var backgroundColor = Color.rgb(91, 45, 190)
    private var textColor = Color.WHITE
    private var fontSize = 100f

    private var titleX = 0.5f
    private var titleY = 0.46f
    private var subtitleX = 0.5f
    private var subtitleY = 0.68f

    private var dragTitle = true
    private var downX = 0f
    private var downY = 0f
    private var startX = 0f
    private var startY = 0f

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.openInputStream(it)?.use { stream ->
                        backgroundBitmap = BitmapFactory.decodeStream(stream)
                    }
                    refreshPreview()
                } catch (_: Exception) {
                    toast("Couldn't load that image")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 18)
            setBackgroundColor(Color.rgb(18, 18, 26))
        }

        val heading = TextView(this).apply {
            text = "ThumbStudio"
            textSize = 28f
            setTextColor(Color.rgb(255, 210, 40))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
        }
        root.addView(heading)

        val info = TextView(this).apply {
            text = "CREATE YOUR NEXT THUMBNAIL"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 14)
        }
        root.addView(info)

        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            setBackgroundColor(Color.DKGRAY)
            contentDescription = "Thumbnail preview. Drag to move the title."
        }

        root.addView(
            preview,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply {
                bottomMargin = 12
            }
        )

        preview.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    startX = if (dragTitle) titleX else subtitleX
                    startY = if (dragTitle) titleY else subtitleY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (preview.width > 0 && preview.height > 0) {
                        val newX = (startX +
                            (event.x - downX) / preview.width).coerceIn(0.05f, 0.95f)
                        val newY = (startY +
                            (event.y - downY) / preview.height).coerceIn(0.05f, 0.95f)

                        if (dragTitle) {
                            titleX = newX
                            titleY = newY
                        } else {
                            subtitleX = newX
                            subtitleY = newY
                        }
                        refreshPreview()
                    }
                    true
                }

                MotionEvent.ACTION_UP -> true
                else -> true
            }
        }

        titleInput = editField("YOUR EPIC TITLE")
        root.addView(titleInput)

        subtitleInput = editField("Add a subtitle...")
        root.addView(subtitleInput)

        titleInput.addTextChangedListener(SimpleWatcher { refreshPreview() })
        subtitleInput.addTextChangedListener(SimpleWatcher { refreshPreview() })

        root.addView(label("TEMPLATES"))
        root.addView(buttonRow(
            listOf("Roblox", "Minecraft", "YouTube")
        ) { choice ->
            when (choice) {
                "Roblox" -> {
                    titleInput.setText("ROBLOX")
                    subtitleInput.setText("NEW ADVENTURE!")
                    backgroundColor = Color.rgb(205, 40, 40)
                    textColor = Color.WHITE
                    fontSize = 100f
                }
                "Minecraft" -> {
                    titleInput.setText("MINECRAFT")
                    subtitleInput.setText("LET'S PLAY!")
                    backgroundColor = Color.rgb(48, 135, 65)
                    textColor = Color.rgb(255, 240, 110)
                    fontSize = 90f
                }
                else -> {
                    titleInput.setText("NEW VIDEO!")
                    subtitleInput.setText("WATCH NOW")
                    backgroundColor = Color.rgb(30, 90, 210)
                    textColor = Color.WHITE
                    fontSize = 100f
                }
            }
            refreshPreview()
        })

        root.addView(label("BACKGROUND COLOR"))
        root.addView(buttonRow(
            listOf("Purple", "Blue", "Red", "Green")
        ) {
            backgroundBitmap = null
            backgroundColor = when (it) {
                "Purple" -> Color.rgb(91, 45, 190)
                "Blue" -> Color.rgb(30, 90, 210)
                "Red" -> Color.rgb(205, 40, 40)
                else -> Color.rgb(35, 135, 65)
            }
            refreshPreview()
        })

        root.addView(label("TEXT COLOR"))
        root.addView(buttonRow(
            listOf("White", "Yellow", "Cyan", "Pink")
        ) {
            textColor = when (it) {
                "White" -> Color.WHITE
                "Yellow" -> Color.YELLOW
                "Cyan" -> Color.CYAN
                else -> Color.rgb(255, 105, 180)
            }
            refreshPreview()
        })

        root.addView(buttonRow(listOf("−  Smaller", "+  Bigger")) {
            fontSize = (fontSize + if (it.startsWith("+")) 10f else -10f)
                .coerceIn(35f, 180f)
            refreshPreview()
        })

        root.addView(buttonRow(listOf("Move Title", "Move Subtitle")) {
            dragTitle = it == "Move Title"
            toast(if (dragTitle) "Drag the title in the preview"
            else "Drag the subtitle in the preview")
        })

        root.addView(buttonRow(listOf("Import Background", "Export PNG")) {
            when (it) {
                "Import Background" -> imagePicker.launch("image/*")
                "Export PNG" -> exportPng()
            }
        })

        setContentView(root)
        refreshPreview()
    }

    private fun editField(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(12, 2, 12, 2)
            setBackgroundColor(Color.rgb(39, 39, 51))
        }
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(2, 8, 2, 4)
        }
    }

    private fun buttonRow(
        names: List<String>,
        action: (String) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            names.forEach { name ->
                val button = Button(this@MainActivity).apply {
                    text = name
                    textSize = 11f
                    isAllCaps = false
                    setOnClickListener { action(name) }
                }
                addView(
                    button,
                    LinearLayout.LayoutParams(0, 46, 1f).apply {
                        setMargins(2, 1, 2, 1)
                    }
                )
            }
        }
    }

    private fun refreshPreview() {
        if (!::preview.isInitialized) return
        val bitmap = renderThumbnail(640, 360)
        val old = preview.drawable
        preview.setImageBitmap(bitmap)
        if (old is android.graphics.drawable.BitmapDrawable) {
            val oldBitmap = old.bitmap
            if (oldBitmap !== bitmap && !oldBitmap.isRecycled) {
                oldBitmap.recycle()
            }
        }
    }

    private fun renderThumbnail(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        canvas.drawColor(backgroundColor)

        backgroundBitmap?.let { bg ->
            val source = Rect(0, 0, bg.width, bg.height)
            val destination = Rect(0, 0, width, height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bg, source, destination, paint)
        }

        val title = titleInput.text.toString()
        val subtitle = subtitleInput.text.toString()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = fontSize * width / 1280f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            style = Paint.Style.FILL
        }

        val outline = Paint(titlePaint).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = maxOf(3f, titlePaint.textSize * 0.07f)
            strokeJoin = Paint.Join.ROUND
        }

        val x = titleX * width
        val y = titleY * height

        canvas.drawText(title, x, y, outline)
        canvas.drawText(title, x, y, titlePaint)

        val subPaint = Paint(titlePaint).apply {
            textSize = titlePaint.textSize * 0.42f
        }
        val subOutline = Paint(outline).apply {
            textSize = subPaint.textSize
            strokeWidth = maxOf(2f, subPaint.textSize * 0.07f)
        }

        canvas.drawText(
            subtitle, subtitleX * width, subtitleY * height, subOutline
        )
        canvas.drawText(
            subtitle, subtitleX * width, subtitleY * height, subPaint
        )

        return bitmap
    }

    private fun exportPng() {
        try {
            val bitmap = renderThumbnail(1280, 720)
            val filename = "ThumbStudio_${System.currentTimeMillis()}.png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/ThumbStudio"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: throw Exception("Could not create image")

            contentResolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw Exception("PNG export failed")
                }
            } ?: throw Exception("Could not open image output")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finish = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, finish, null, null)
            }

            bitmap.recycle()
            toast("PNG saved to Pictures/ThumbStudio!")
        } catch (e: Exception) {
            toast("Export failed: ${e.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private class SimpleWatcher(
        private val changed: () -> Unit
    ) : android.text.TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?, start: Int, count: Int, after: Int
        ) {}
        override fun onTextChanged(
            s: CharSequence?, start: Int, before: Int, count: Int
        ) {
            changed()
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
