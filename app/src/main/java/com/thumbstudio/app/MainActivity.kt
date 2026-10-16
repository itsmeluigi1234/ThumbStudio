
package com.thumbstudio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var preview: ImageView
    private lateinit var titleInput: EditText
    private var backgroundBitmap: Bitmap? = null

    companion object {
        private const val PICK_IMAGE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 24)
        window.navigationBarColor = Color.rgb(16, 16, 24)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(16, 16, 24))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val heading = TextView(this).apply {
            text = "ThumbStudio"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(heading)

        val subtitle = TextView(this).apply {
            text = "Create your next Roblox thumbnail"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 20)
        }
        content.addView(subtitle)

        preview = ImageView(this).apply {
            setBackgroundColor(Color.rgb(42, 35, 70))
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            minimumHeight = 200
            setImageBitmap(createPreview())
        }

        content.addView(
            preview,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 220
            )
        )

        titleInput = EditText(this).apply {
            hint = "Enter thumbnail title"
            setText("MY ROBLOX GAME")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 20f
            maxLines = 1
            setSingleLine(true)
        }

        content.addView(titleInput)

        addButton(content, "Import Background Image") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_IMAGE)
        }

        addButton(content, "Update Preview") {
            preview.setImageBitmap(createPreview())
        }

        addButton(content, "Export Thumbnail") {
            exportThumbnail()
        }

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun createPreview(): Bitmap {
        val width = 1280
        val height = 720
        val bitmap = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.rgb(42, 35, 70))

        backgroundBitmap?.let {
            val source = Bitmap.createScaledBitmap(it, width, height, true)
            canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 100f
            typeface = Typeface.create(
                "sans-serif-black", Typeface.BOLD
            )
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 4f, 4f, Color.BLACK)
        }

        val title = titleInput.text.toString().ifBlank {
            "MY ROBLOX GAME"
        }.uppercase()

        val words = title.take(24)
        canvas.drawText(words, width / 2f, height * 0.82f, paint)

        return bitmap
    }

    @Deprecated("Uses the classic Android image picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return

            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    backgroundBitmap = BitmapFactory.decodeStream(stream)
                }
                preview.setImageBitmap(createPreview())
                Toast.makeText(
                    this, "Image imported!", Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this, "Couldn't open that image", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportThumbnail() {
        try {
            val bitmap = createPreview()
            val uri = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "ThumbStudio-${System.currentTimeMillis()}",
                "Created with ThumbStudio"
            )

            if (uri != null) {
                Toast.makeText(
                    this,
                    "Thumbnail saved to your gallery!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this, "Couldn't save thumbnail",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this, "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
