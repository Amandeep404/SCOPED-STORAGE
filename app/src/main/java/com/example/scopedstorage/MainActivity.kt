package com.example.scopedstorage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.scopedstorage.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var internalStorageAdapter : InternalStoragePhotoAdapter
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        internalStorageAdapter = InternalStoragePhotoAdapter {
            val isPhotoDeletedFromInternalStorage = deletePhotoFromInternalStorage(it.name)
            if (isPhotoDeletedFromInternalStorage){
                loadPhotosFromInternalStorageIntoRecyclerView()
                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this, "Error in deleting photo from private folder", Toast.LENGTH_SHORT).show()
            }
        }

        setUpInternalStorageRecyclerView()
        loadPhotosFromInternalStorageIntoRecyclerView()

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){
             // After photo is taken

            val isPrivate = binding.switchPrivate.isChecked
            if (isPrivate){
               val isPhotoSavedToInternalStorageSuccessfully = savePhotoToInternalStorage(UUID.randomUUID().toString(), it!!)
                if (isPhotoSavedToInternalStorageSuccessfully){
                    loadPhotosFromInternalStorageIntoRecyclerView()
                    Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()

                }else{
                    Toast.makeText(this, "Error in saving photo to Private folder", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnTakePhoto.setOnClickListener{
            takePhoto.launch()
        }

    }

    private suspend fun loadPhotosFromInternalStorage() : List<InternalStoragePhoto>{
        return withContext(Dispatchers.IO){
            val files = filesDir.listFiles()
            files?.filter {
                  it.canRead() && it.isFile && it.name.endsWith(".jpg")
            }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            }?: listOf()
        }
    }

    private fun savePhotoToInternalStorage(fileName : String, bmp : Bitmap): Boolean{
        return try {
            openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)){
                    throw IOException("Could not save the Bitmap")
                }
            }
            true
        }
        catch (e : IOException){
            e.printStackTrace()
            false
        }
    }

    private fun deletePhotoFromInternalStorage(fileName: String) : Boolean{
        return try {
                deleteFile(fileName)
        }
        catch (e : Exception){
            e.printStackTrace()
            false
        }
    }

    private fun setUpInternalStorageRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = internalStorageAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private fun loadPhotosFromInternalStorageIntoRecyclerView(){
        lifecycleScope.launch{
            val photos = loadPhotosFromInternalStorage()
            internalStorageAdapter.submitList(photos)
        }
    }

}