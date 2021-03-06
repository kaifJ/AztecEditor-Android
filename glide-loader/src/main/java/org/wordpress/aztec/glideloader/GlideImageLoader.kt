package org.wordpress.aztec.glideloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import org.wordpress.aztec.Html
import org.wordpress.aztec.glideloader.extensions.upscaleTo
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL

class GlideImageLoader(private val context: Context, val customProps: HashMap<Any, Any>) : Html.ImageGetter {

    override fun loadImage(source: String, callbacks: Html.ImageGetter.Callbacks, maxWidth: Int) {
        loadImage(source, callbacks, maxWidth, 0)
    }

    override fun loadImage(source: String, callbacks: Html.ImageGetter.Callbacks, maxWidth: Int, minWidth: Int) {
        val headerBuilder = LazyHeaders.Builder()

        //add headers to GlideURl only if image is not already downloaded
        val imageFile = File(source)
        var glideURL: Any? = null
        var createGlideURL = false

        if(source.contains("file://", ignoreCase = false)){
            source = source.replace("file://", "")
        }

        if(! imageFile.exists() && customProps.containsKey(("headers"))){
            val headerProps = customProps.get("headers") as HashMap<Any, Any>
            for((key, value) in headerProps){
                headerBuilder.addHeader(""+key, ""+ value)
            }

           glideURL = GlideUrl(source, headerBuilder.build())
        }else{
            glideURL = source
        }

        Glide.with(context).asBitmap().load(glideURL).into(object : Target<Bitmap> {
            override fun onLoadStarted(placeholder: Drawable?) {
                callbacks.onImageLoading(placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                callbacks.onImageFailed()
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                //Upscaling bitmap only for demonstration purposes.
                //This should probably be done somewhere more appropriate for Glide (?).
                if (resource.width < minWidth) {
                    return callbacks.onImageLoaded(BitmapDrawable(context.resources, resource.upscaleTo(minWidth)))
                }

                // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                // to correctly set the input density to 160 ourselves.
                resource.density = DisplayMetrics.DENSITY_DEFAULT

                if(imageFile.exists()){
                    return callbacks.onImageLoaded(BitmapDrawable(context.resources, resource))
                }

                val destinationPath = context.filesDir.absolutePath + "/RTF/"
                var file = File(destinationPath)
                if(!file.exists()){
                    file.mkdirs()
                }
                file = File(destinationPath, URL(source).path.replace("/", "")+"")
                try {
                    val out = FileOutputStream(file)
                    resource.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.flush()
                    out.close()
                } catch(e: Exception){}

                callbacks.onImageLoaded(BitmapDrawable(context.resources, resource))
            }

            override fun onLoadCleared(placeholder: Drawable?) {}

            override fun getSize(cb: SizeReadyCallback) {
                cb.onSizeReady(maxWidth, Target.SIZE_ORIGINAL)
            }

            override fun removeCallback(cb: SizeReadyCallback) {
            }

            override fun setRequest(request: Request?) {
            }

            override fun getRequest(): Request? {
                return null
            }

            override fun onStart() {
            }

            override fun onStop() {
            }

            override fun onDestroy() {
            }
        })
    }
}
