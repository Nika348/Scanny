package com.example.scanny.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.scanny.BoundingBoxOverlay
import com.example.scanny.R
import com.example.scanny.databinding.FragmentPhotoBinding
import com.example.scanny.models.Detector
import com.example.scanny.models.ModelConfiguration
import com.example.scanny.models.TFDetector
import java.io.IOException

class PhotoFragment : Fragment(R.layout.fragment_photo) {

    private var _binding: FragmentPhotoBinding? = null
    private val binding
        get() = _binding ?: throw NullPointerException("FragmentPhotoBinding is null")

    private val args by navArgs<PhotoFragmentArgs>()

    private lateinit var tfDetector: Detector
    private lateinit var overlayView: BoundingBoxOverlay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PhotoFragment", "Received photoPath: $args.photoPath")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPhotoBinding.bind(view)

        // Инициализация TFDetector
        val assets = requireContext().assets
        val modelConfiguration = ModelConfiguration(640, 480)
        tfDetector = TFDetector(assets, modelConfiguration)
        overlayView = BoundingBoxOverlay(binding.photo)

        // Загрузка и отображение фото
        loadPhoto()
    }

    private fun loadPhoto() {
        // Загрузка фото по указанному пути и установка в ImageView
        val arguments = args.photoPath
        binding.photo.load(arguments)
        Log.d("PhotoFragment", "Photo loaded: ${binding.photo}, args: ${args.photoPath}")

         //Обнаружение объектов на фото
        if(arguments != null) {
            val bitmap = getBitmapFromUri(arguments)

            if (bitmap != null) {
                val detections = tfDetector.detect(bitmap)

                // Передача результатов обнаружения в OverlayView
                overlayView.drawBoundingBoxes(bitmap, detections)
                Log.d("PhotoFragment", "Drawable is successful")

            } else {
                Log.e("PhotoFragment", "Drawable is null")
            }
        } else {
            Log.e("PhotoFragment", "Invalid Uri")
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}