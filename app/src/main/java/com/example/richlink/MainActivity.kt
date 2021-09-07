package com.example.richlink

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.richlink.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this

        setContentView(binding.root)

        binding.processUrl.setOnClickListener {
            val url = binding.urlBox.text.toString().trim().lowercase()
            if (url.isNotBlank()) {
                fetchPreview(url)
            }
        }
    }

    private fun fetchPreview(url: String) {
        lifecycleScope.launch {
            var hasPreviewData: Boolean = false
            var title: String? = null
            var desc: String? = null
            var image: String? = null

            withContext(Dispatchers.IO) {
                val response = Jsoup.connect(url).ignoreHttpErrors(true).execute()
                when (response.statusCode()) {
                    200 -> {
                        val document = response.parse()
                        if (document != null) {
                            val titleElements = document.select("meta[property=og:title]")
                            if (!titleElements.isNullOrEmpty()) {
                                title = titleElements[0].attr("content")
                            }

                            if (title.isNullOrBlank()) title = document.title()

                            var descElements = document.select("meta[property=og:description]")
                            if (!descElements.isNullOrEmpty()) {
                                desc = descElements[0].attr("content")
                            }

                            if (desc.isNullOrBlank()) {
                                descElements = document.select("meta[name=description]")
                                if (!descElements.isNullOrEmpty()) {
                                    desc = descElements[0].attr("content")
                                }
                            }

                            if(desc.isNullOrBlank()){
                                desc = response.url().toString()
                            }

                            val imageElements = document.select("link[rel=shortcut icon]")
                            if (!imageElements.isNullOrEmpty()) {
                                image = imageElements[0].attr("href")
                                if (!image.isNullOrBlank()) {
                                    if (image!!.contains("https") || image!!.contains("http")) {

                                    } else {
                                        val updatedUrl = url.let {
                                            if (it[it.length - 1] == '/') it.substring(
                                                0,
                                                it.length - 1
                                            ) else it
                                        }

                                        val updateHref = image!!.let {
                                            if (it[0] == '/') it.substring(
                                                1,
                                                it.length
                                            ) else
                                                it
                                        }

                                        image = "$updatedUrl/$updateHref"
                                    }
                                }
                            }

                            hasPreviewData = true

                        } else {
                            hasPreviewData = false
                        }
                    }
                    else -> hasPreviewData = false
                }
            }

            if (hasPreviewData) {
                showPreview(title, desc, image)
            } else {
                showToast("No data found")
                binding.previewContainer.visibility = View.GONE
            }
        }
    }

    private fun showPreview(title: String?, desc: String?, image: Any?) {
        binding.apply {
            previewContainer.visibility = View.VISIBLE

            previewTitle.text = title
            previewDesc.text = desc

            Glide.with(previewThumbnail).load(image).into(previewThumbnail)
        }
    }
}


fun Activity.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}