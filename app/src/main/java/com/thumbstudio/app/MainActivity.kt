
package com.thumbstudio.app

import android.app.Activity
import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : Activity() {
    private lateinit var artwork: ArtworkView
    private lateinit var titleInput: EditText
    private var importedBitmap: Bitmap? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use {
                        importedBitmap = BitmapFactory.decodeStream(it)
                    }
                    artwork.invalidate()
                } catch (_: Exception) {
                    Toast.makeText(this, "Couldn't open image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 24)
        window.navigationBarColor = Color.rgb(16, 16, 24)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.rgb(16, 16, 24))
        }

        page.addView(TextView(this).apply {
            text = "ThumbStudio"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        page.addView(TextView(this).apply {
            text = "ROBLOX THUMBNAIL EDITOR • 16:9"
            textSize = 11f
            setTextColor(Color.rgb(166, 164, 190))
            setPadding(0, dp(4), 0, dp(14))
        })

        titleInput = EditText(this).apply {
            hint = "Enter thumbnail title"
            setText("EPIC ADVENTURE!")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 16f
            singleLine = true
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.rgb(35, 34, 49))
        }
        page.addView(titleInput, LinearLayout.LayoutParams(-1, dp(50)))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        }

        buttons.addView(Button(this).apply {
            text = "Import image"
            setOnClickListener { pickImage.launch("image/*") }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))

        buttons.addView(Button(this).apply {
            text = "Export PNG"
            setOnClickListener { exportImage() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))

        page.addView(buttons)

        artwork = ArtworkView()
        page.addView(artwork, LinearLayout.LayoutParams(-1, dp(230)))

        page.addView(TextView(this).apply {
            text = "Import an image, edit the title, and export your thumbnail."
            textSize = 12f
            setTextColor(Color.rgb(166, 164, 190))
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(8))
        })

        val scroll = ScrollView(this)
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun exportImage() {
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            artwork.drawArtwork(
                Canvas(bitmap), 1280f, 720f,
                titleInput.text.toString(), importedBitmap
            )

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME,
                    "ThumbStudio_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ThumbStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )

            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    contentResolver.update(uri, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null)
                }
                Toast.makeText(
                    this, "Saved to Pictures/ThumbStudio", Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Couldn't save PNG", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            bitmap?.recycle()
        }
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    private inner class ArtworkView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 255, 255, 255)
            strokeWidth = 1f
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(
                "sans-serif-black", Typeface.BOLD
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawArtwork(
                canvas, width.toFloat(), height.toFloat(),
                titleInput.text.toString(), importedBitmap
            )
        }

        fun drawArtwork(
            canvas: Canvas, w: Float, h: Float,
            title: String, image: Bitmap?
        ) {
            if (w <= 0f || h <= 0f) return
            val saved = canvas.save()
            canvas.scale(w / 1280f, h / 720f)

            paint.shader = LinearGradient(
                0f, 0f, 1280f, 720f,
                intArrayOf(
                    Color.rgb(45, 30, 105),
                    Color.rgb(16, 112, 143),
                    Color.rgb(244, 106, 81)
                ),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, 1280f, 720f, paint)
            paint.shader = null

            paint.color = Color.argb(75, 255, 255, 255)
            canvas.drawCircle(1020f, 150f, 170f, paint)
            paint.color = Color.argb(45, 255, 220, 120)
            canvas.drawCircle(1020f, 150f, 125f, paint)

            for (x in 0..1280 step 64)
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), 720f, grid)
            for (y in 0..720 step 64)
                canvas.drawLine(0f, y.toFloat(), 1280f, y.toFloat(), grid)

            if (image != null && !image.isRecycled) {
                val ratio = image.width.toFloat() / image.height
                val target = 1280f / 720f
                val src = if (ratio > target) {
                    val crop = (image.height * target).toInt()
                    Rect(
                        (image.width - crop) / 2, 0,
                        (image.width + crop) / 2, image.height
                    )
                } else {
                    val crop = (image.width / target).toInt()
                    Rect(
                        0, (image.height - crop) / 2,
                        image.width, (image.height + crop) / 2
                    )
                }
                val imagePaint = Paint(
                    Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
                ).apply { alpha = 210 }

                canvas.drawBitmap(
                    image, src, RectF(0f, 0f, 1280f, 720f), imagePaint
                )
                paint.color = Color.argb(75, 0, 0, 0)
                canvas.drawRect(0f, 0f, 1280f, 720f, paint)
            }

            val safeTitle = title.ifBlank { "YOUR TITLE HERE" }.uppercase()
            val display = if (safeTitle.length > 28)
                safeTitle.take(25) + "..." else safeTitle

            titlePaint.textSize = when {
                display.length > 20 -> 74f
                display.length > 13 -> 92f
                else -> 112f
            }
            titlePaint.style = Paint.Style.STROKE
            titlePaint.strokeJoin = Paint.Join.ROUND
            titlePaint.strokeWidth = 18f
            titlePaint.color = Color.BLACK
            canvas.drawText(display, 640f, 570f, titlePaint)

            titlePaint.style = Paint.Style.FILL
            titlePaint.color = Color.WHITE
            canvas.drawText(display, 640f, 570f, titlePaint)

            titlePaint.textSize = 28f
            titlePaint.color = Color.rgb(255, 225, 110)
            titlePaint.setShadowLayer(4f, 2f, 3f, Color.BLACK)
            canvas.drawText(
                "THUMBSTUDIO • CREATE SOMETHING EPIC",
                640f, 640f, titlePaint
            )
            titlePaint.clearShadowLayer()
            canvas.restoreToCount(saved)
        }
    }
}
