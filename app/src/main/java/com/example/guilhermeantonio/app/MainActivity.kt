package com.example.guilhermeantonio.app

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    private val permission_code = 1
    private val camera_code = 1
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun processarFoto(view: View) {

        progressDialog = ProgressDialog.show(this@MainActivity, "Photo", "Processando...", false, false)

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("image.jpg")

        foto.isDrawingCacheEnabled = true
        foto.buildDrawingCache()
        val bitmap = foto.drawingCache
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener({

        }).addOnSuccessListener({ taskSnapshot ->

            val url = taskSnapshot.downloadUrl

            server(url.toString())


        })


    }

    fun tirarFoto(view: View) {

        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), permission_code)
            }
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            permission_code -> {

                openCamera()

            }
        }
    }

    fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, camera_code)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == camera_code && resultCode == Activity.RESULT_OK) {

            button_processar_foto.visibility = View.VISIBLE

            val extras = data?.extras
            val imageBitmap = extras?.get("data") as Bitmap
            foto.setImageBitmap(imageBitmap)
        }

    }

    private fun server(url_server: String?) {

        val client: Retrofit = Retrofit.Builder()
                .baseUrl("http://172.10.10.102:3000")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val call = client.create(ApiInterface::class.java).photo(url_server.toString())

//        Log.v("TAG", url_server!!.replace("\"", ""))

        call.enqueue(object : Callback<Data> {
            override fun onFailure(call: Call<Data>?, t: Throwable?) {

                Log.v("TAG", t?.message)

            }

            override fun onResponse(call: Call<Data>?, response: Response<Data>?) {

                if (response?.isSuccessful!!) {
                    progressDialog?.dismiss()
                    linear.visibility = View.VISIBLE

                    val list: ArrayList<String> = ArrayList()

                    response.body()?.description?.tags?.forEach { list.add(it) }

                    val arrayAdapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, android.R.id.text1, list)
                    listTags.adapter = arrayAdapter

                    val listCaption: ArrayList<String> = ArrayList()

                    response.body()?.description?.captions?.forEach { listCaption.add("${it.text} - ${it.confidence}") }

                    val arrayAdapterCaptions = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, android.R.id.text1, listCaption)
                    listCaptions.adapter = arrayAdapterCaptions

                    val listCategory: ArrayList<String> = ArrayList()

                    response.body()?.categories?.forEach { listCategory.add("${it.score} - ${it.name}") }

                    val arrayAdapterCategory = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, android.R.id.text1, listCategory)
                    listCategorias.adapter = arrayAdapterCategory


                } else {

                    Toast.makeText(this@MainActivity, "wait for server", Toast.LENGTH_SHORT).show()

                    progressDialog?.dismiss()

                }

            }

        })

    }

    interface ApiInterface {
        @POST("/processamento/")
        fun photo(@Body url: String): Call<Data>


    }

    class Data {

        @SerializedName("categories")
        @Expose
        var categories: List<Category>? = null
        @SerializedName("description")
        @Expose
        var description: Description? = null
        @SerializedName("requestId")
        @Expose
        var requestId: String? = null
        @SerializedName("metadata")
        @Expose
        var metadata: Metadata? = null
        @SerializedName("color")
        @Expose
        var color: Color? = null

    }

    class Caption {

        @SerializedName("confidence")
        @Expose
        var confidence: Double? = null
        @SerializedName("text")
        @Expose
        var text: String? = null
    }

    class Category {
        @SerializedName("name")
        @Expose
        var name: String? = null
        @SerializedName("score")
        @Expose
        var score: Float? = null
    }

    class Color {
        @SerializedName("dominantColorForeground")
        @Expose
        var dominantColorForeground: String? = null
        @SerializedName("dominantColorBackground")
        @Expose
        var dominantColorBackground: String? = null
        @SerializedName("dominantColors")
        @Expose
        var dominantColors: List<String>? = null
        @SerializedName("accentColor")
        @Expose
        var accentColor: String? = null
        @SerializedName("isBWImg")
        @Expose
        var isBWImg: Boolean? = null
    }

    class Description {
        @SerializedName("tags")
        @Expose
        var tags: List<String>? = null
        @SerializedName("captions")
        @Expose
        var captions: List<Caption>? = null
    }

    class Metadata {
        @SerializedName("width")
        @Expose
        var width: Int? = null
        @SerializedName("height")
        @Expose
        var height: Int? = null
        @SerializedName("format")
        @Expose
        var format: String? = null
    }


}
