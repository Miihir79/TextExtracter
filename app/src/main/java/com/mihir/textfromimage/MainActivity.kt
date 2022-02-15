package com.mihir.textfromimage

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.StringBuilder
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap->
        if(bitmap != null){
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this)
        }
    }
    private val onresult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        Log.i("TAG", "this is the result: ${result.data} ${result.resultCode}")
        onResultRecieved(CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE,result)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        window.statusBarColor = Color.BLACK

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        btn_capture.setOnClickListener{
            Dexter.withContext(this).withPermissions(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.i("TAG", "onPermissionsChecked: all permissions granted")
                        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(this@MainActivity)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Please allow all the permissions",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest?>?,
                    permissionToken: PermissionToken?
                ) {
                }
            }).check()

        }

        imageView.setOnClickListener {
            val clipboardManager :ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = ClipData.newPlainText("Text",edTxt_text.text)
            clipboardManager.setPrimaryClip(data)
            Toast.makeText(this, "Copied to ClipBoard",Toast.LENGTH_LONG).show()
        }

    }

    private fun onResultRecieved(requestCode: Int, result: ActivityResult?) {
        when(requestCode){
            // this does not work
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE ->{
                Log.i("TAG", "onResultRecieved: it goes inside")
                val resultCropImage= CropImage.getActivityResult(result?.data)
                if (result?.resultCode == Activity.RESULT_OK){
                    resultCropImage.uri?.let {
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it))
                        extractText(bitmap)
                    }

                }else{
                    Log.e("TAG", "onActivityResult: ${resultCropImage.error}" )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK){
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,result.uri)
                extractText(bitmap)
            }
        }

        /*if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            Log.i("TAG", "onActivityResult: $requestCode $resultCode")
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK){
                Log.i("TAG", "onActivityResult: ${result.uri}")
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(result.uri))
                extractText(bitmap)
            }
        }
        else{
            Log.i("TAG", "onActivityResult: $requestCode")
        }*/
    }

    private fun extractText(bitmap: Bitmap){
        val recognizer = TextRecognizer.Builder(this).build()
        if (!recognizer.isOperational){
            Toast.makeText(this,"Some error",Toast.LENGTH_LONG).show()
        }else{
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val stringish = recognizer.detect(frame)
            val stringBuilder = StringBuilder()
            var i =0
            while (i<stringish.size()-1){
                i++
                val textBlock = stringish.valueAt(i)
                stringBuilder.append(textBlock.value)
                stringBuilder.append("\n")
            }
            edTxt_text.setText(stringBuilder.toString())
            btn_capture.text = "Retake"
            imageView.visibility = View.VISIBLE
        }
    }
}