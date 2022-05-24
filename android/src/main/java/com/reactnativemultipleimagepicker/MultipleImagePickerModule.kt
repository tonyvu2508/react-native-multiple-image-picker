package com.reactnativemultipleimagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.*
import com.labters.documentscanner.ImageCropActivity
import com.labters.documentscanner.helpers.ScannerConstants
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.app.IApp
import com.luck.picture.lib.app.PictureAppMaster
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.engine.PictureSelectorEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.luck.picture.lib.style.PictureParameterStyle
import java.io.*
import java.util.*


@Suppress("INCOMPATIBLE_ENUM_COMPARISON", "UNCHECKED_CAST")
class MultipleImagePickerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), IApp, ActivityEventListener {

    override fun getName(): String {
        return "MultipleImagePicker"
    }

    private var selectedAssets: List<LocalMedia> = ArrayList()
    private var mPictureParameterStyle: PictureParameterStyle? = null
    private var singleSelectedMode: Boolean = false
    private var maxVideoDuration: Int = 60
    private var numberOfColumn: Int = 3
    private var maxSelectedAssets: Int = 20
    private var mediaType: String = "all"
    private var isPreview: Boolean = true
    private var isExportThumbnail: Boolean = false
    private var maxVideo: Int = 20
    private var callback: Promise? = null

    @ReactMethod
    fun openCropPicker(options: ReadableMap?, promise: Promise){
        val activity = currentActivity
        callback = promise
        reactApplicationContext.addActivityEventListener(this)
        val path = options?.getString("doneTitle")
        val uri = Uri.fromFile(File(path))
        ScannerConstants.initImageBitmap = uri.toString()
        var bitmap = MediaStore.Images.Media.getBitmap(reactApplicationContext.contentResolver, uri)
        bitmap = path?.let { checkBitmap(bitmap, it) };
        ScannerConstants.selectedImageBitmap = bitmap
        activity!!.startActivityForResult(Intent(activity, ImageCropActivity::class.java), 123)
    }

    @ReactMethod
    fun openPicker(options: ReadableMap?, promise: Promise): Unit {
        callback = promise
        reactApplicationContext.addActivityEventListener(this)
        PictureAppMaster.getInstance().app = this
        val activity = currentActivity
        setConfiguration(options)
        PictureSelector.create(activity)
            .openGallery(PictureMimeType.ofImage())
            .loadImageEngine(GlideEngine.createGlideEngine())
            .isZoomAnim(true)
            .isPageStrategy(true, 50)
            .imageEngine(GlideEngine.createGlideEngine())
            .selectionMode(PictureConfig.SINGLE)
            .forResult(object : OnResultCallbackListener<Any?> {
                override fun onResult(result: MutableList<Any?>?) {
                    val item: LocalMedia = result?.get(0) as LocalMedia
                    val uri = Uri.fromFile(File(item.realPath))
                    ScannerConstants.initImageBitmap = uri.toString()
                    var bitmap = MediaStore.Images.Media.getBitmap(reactApplicationContext.contentResolver, uri)
                    bitmap = checkBitmap(bitmap, item.realPath);
                    Log.d("xxxx realPath", item.realPath);
                    ScannerConstants.selectedImageBitmap = bitmap
                    activity!!.startActivityForResult(Intent(activity, ImageCropActivity::class.java), 123)
                }

                override fun onCancel() {
                    promise.reject("user cancel")
                }
            })
    }

    fun checkBitmap(bitmap: Bitmap, realPath: String):Bitmap?{
        try {
            val exif: ExifInterface = ExifInterface(realPath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90F)
            } else if (orientation == 3) {
                matrix.postRotate(180F)
            } else if (orientation == 8) {
                matrix.postRotate(270F)
            }
            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
            ) // rotating bitmap

        } catch (e: java.lang.Exception) {
        }
        return bitmap;
    }

    fun getExifInterface(context: Context, uri: Uri): ExifInterface? {
        try {
            val path = uri.toString()
            if (path.startsWith("file://")) {
                return ExifInterface(path)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (path.startsWith("content://")) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    return ExifInterface(inputStream!!)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun getExifAngle(context: Context?, uri: Uri?): Float {
        return try {
            val exifInterface = getExifInterface(context!!, uri!!) ?: return -1f
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                ExifInterface.ORIENTATION_NORMAL -> 0f
                ExifInterface.ORIENTATION_UNDEFINED -> -1f
                else -> -1f
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1f
        }
    }

    private fun setConfiguration(options: ReadableMap?) {
        if (options != null) {
            handleSelectedAssets(options)
            singleSelectedMode = options.getBoolean("singleSelectedMode")
            maxVideoDuration = options.getInt("maxVideoDuration")
            numberOfColumn = options.getInt("numberOfColumn")
            maxSelectedAssets = options.getInt("maxSelectedAssets")
            mediaType = options.getString("mediaType").toString()
            isPreview = options.getBoolean("isPreview")
            isExportThumbnail = options.getBoolean("isExportThumbnail")
            maxVideo = options.getInt("maxVideo")
            mPictureParameterStyle = getStyle(options)
        }
    }

    private fun getStyle(options: ReadableMap): PictureParameterStyle? {
        val pictureStyle = PictureParameterStyle()
        pictureStyle.pictureCheckedStyle = R.drawable.picture_selector

        //bottom style
        pictureStyle.pictureCompleteText = options.getString("doneTitle")
        pictureStyle.picturePreviewText = "プレビュー"
        pictureStyle.pictureUnCompleteText = "選んでください"
        pictureStyle.pictureUnPreviewText = "プレビュー"
        pictureStyle.isOpenCheckNumStyle = true
        pictureStyle.isCompleteReplaceNum = true
        pictureStyle.pictureCompleteTextSize = 16
        pictureStyle.pictureCheckNumBgStyle = R.drawable.num_oval_orange
        pictureStyle.pictureCompleteTextColor = Color.parseColor("#ffffff")
        pictureStyle.pictureNavBarColor = Color.parseColor("#000000")
        pictureStyle.pictureBottomBgColor = Color.parseColor("#393a3e")
        //preview Style
        pictureStyle.picturePreviewBottomBgColor = Color.parseColor("#000000")
        pictureStyle.pictureUnPreviewTextColor = Color.parseColor("#ffffff")
        //header
        pictureStyle.pictureTitleDownResId = R.drawable.picture_icon_arrow_down
        pictureStyle.pictureCancelTextColor = Color.parseColor("#393a3e")
        pictureStyle.pictureStatusBarColor = Color.parseColor("#393a3e")
        pictureStyle.pictureTitleBarBackgroundColor = Color.parseColor("#393a3e")
        return pictureStyle
    }

    private fun handleSelectedAssets(options: ReadableMap?) {
        if (options?.hasKey("selectedAssets")!!) {
            val assetsType = options.getType("selectedAssets")
            if (assetsType == ReadableType.Array) {
                val assets: ReadableNativeArray = options.getArray("selectedAssets") as ReadableNativeArray
                if (assets.size() > 0) {
                    val list = mutableListOf<LocalMedia>()
                    for (i in 0 until assets.size()) {
                        val asset: ReadableNativeMap = assets.getMap(i) as ReadableNativeMap
                        val localMedia: LocalMedia = handleSelectedAssetItem(asset)
                        list.add(localMedia)
                    }
                    selectedAssets = list
                    return
                }
                selectedAssets = emptyList()
            }
            if (assetsType == ReadableType.Map) {
                println("type Map")
            }
        }
    }

    private fun handleSelectedAssetItem(asset: ReadableNativeMap): LocalMedia {
        val id: Long = asset.getDouble("localIdentifier").toLong()
        val path: String? = asset.getString("path")
        val realPath: String? = asset.getString("realPath")
        val fileName: String? = asset.getString("fileName")
        val parentFolderName: String? = asset.getString("parentFolderName")
        val duration: Long = asset.getDouble("duration").toLong()
        val chooseModel: Int = asset.getInt("chooseModel")
        val mimeType: String? = asset.getString("mine")
        val width: Int = asset.getInt("width")
        val height: Int = asset.getInt("height")
        val size: Long = asset.getDouble("size").toLong()
        val bucketId: Long = asset.getDouble("bucketId").toLong()
        val dateAddedColumn:Long = asset.getDouble("dateAddedColumn").toLong()
        val localMedia = LocalMedia(id, path, realPath, fileName, parentFolderName, duration, chooseModel, mimeType, width, height, size, bucketId, dateAddedColumn)
        return localMedia
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createAttachmentResponse(item: LocalMedia): WritableMap {
        val media: WritableMap = WritableNativeMap()
        val type: String = if (item.mimeType.startsWith("video/")) "video" else "image"
        media.putString("path", item.path)
        media.putString("realPath", item.realPath)
        media.putString("fileName", item.fileName)
        media.putInt("width", item.width)
        media.putInt("height", item.height)
        media.putString("mine", item.mimeType)
        media.putString("type", type)
        media.putInt("localIdentifier", item.id.toInt())
        media.putInt("position", item.position)
        media.putInt("chooseModel", item.chooseModel)
        media.putDouble("duration", item.duration.toDouble())
        media.putDouble("size", item.size.toDouble())
        media.putDouble("bucketId", item.bucketId.toDouble())
        media.putString("parentFolderName", item.parentFolderName)
        if (type === "video" && isExportThumbnail) {
            val thumbnail = createThumbnail(item.realPath)
            println("thumbnail: $thumbnail")
            media.putString("thumbnail", thumbnail)
        }
        return media
    }

    private fun createThumbnail(filePath: String): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val image = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        val fullPath: String = reactApplicationContext.applicationContext.cacheDir.absolutePath.toString() + "/thumbnails"
        try {
            val dir = fullPath.let { createDirIfNotExists(it) }
            var fOut: OutputStream? = null
            val fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg"
            print("fileName $fileName")
            val file = File(fullPath, fileName)
            file.createNewFile()
            fOut = FileOutputStream(file)

            // 100 means no compression, the lower you go, the stronger the compression
            image?.compress(Bitmap.CompressFormat.JPEG, 50, fOut)
            fOut.flush()
            fOut.close()

            return "file://$fullPath/$fileName"
        } catch (e: Exception) {
            println("Error: " + e?.message)
            return ""
        }
    }
    private fun createDirIfNotExists(path: String): File {
        val dir = File(path)
        if (dir.exists()) {
            return dir
        }
        try {
            dir.mkdirs()
            // Add .nomedia to hide the thumbnail directory from gallery
            val noMedia = File(path, ".nomedia")
            noMedia.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return dir
    }

    override fun getAppContext(): Context {
        return reactApplicationContext
    }

    override fun getPictureSelectorEngine(): PictureSelectorEngine {
        return PictureSelectorEngineImp()
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 123 && resultCode == Activity.RESULT_OK){
            val TOP_LEFT_X = data?.getStringExtra(ImageCropActivity.POINT_X1);
            val TOP_LEFT_Y = data?.getStringExtra(ImageCropActivity.POINT_Y1);

            val TOP_RIGHT_X = data?.getStringExtra(ImageCropActivity.POINT_X2);
            val TOP_RIGHT_Y = data?.getStringExtra(ImageCropActivity.POINT_Y2);

            val BOTTOM_LEFT_X = data?.getStringExtra(ImageCropActivity.POINT_X3);
            val BOTTOM_LEFT_Y = data?.getStringExtra(ImageCropActivity.POINT_Y3);

            val BOTTOM_RIGHT_X = data?.getStringExtra(ImageCropActivity.POINT_X4);
            val BOTTOM_RIGHT_Y = data?.getStringExtra(ImageCropActivity.POINT_Y4);

            val points: WritableArray = WritableNativeArray()
            val myUri = getImageUri(appContext, ScannerConstants.selectedImageBitmap)
            points.pushString(ScannerConstants.initImageBitmap)
            points.pushString(TOP_LEFT_X)
            points.pushString(TOP_LEFT_Y)
            points.pushString(TOP_RIGHT_X)
            points.pushString(TOP_RIGHT_Y)
            points.pushString(BOTTOM_LEFT_X)
            points.pushString(BOTTOM_LEFT_Y)
            points.pushString(BOTTOM_RIGHT_X)
            points.pushString(BOTTOM_RIGHT_Y)
            points.pushString(myUri.toString())
            Log.d("xxxxx", points.toString());
            callback?.resolve(points)
            Handler(Looper.getMainLooper()).postDelayed(
                Runnable {
                    reactApplicationContext.contentResolver.delete(myUri, null, null)
                   },
                3000
            )
        }else{
            callback?.resolve("cancel")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d("xxxx intent", intent.toString())
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

}

