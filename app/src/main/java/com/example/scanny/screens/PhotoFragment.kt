package com.example.scanny.screens

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.scanny.R
import com.example.scanny.databinding.FragmentPhotoBinding
import java.io.File

class PhotoFragment : Fragment(R.layout.fragment_photo) {

    private var _binding: FragmentPhotoBinding? = null
    private val binding
        get() = _binding ?: throw NullPointerException("FragmentPhotoBinding is null")

    private val args by navArgs<PhotoFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение пути к сохраненному фото из аргументов фрагмента
        Log.d("PhotoFragment", "Received photoPath: $args.photoPath")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPhotoBinding.bind(view)

        // Загрузка и отображение фото
        loadPhoto()
    }

    private fun loadPhoto() {
        // Загрузка фото по указанному пути и установка в ImageView
        binding.photo.load(args.photoPath)
    }

    companion object {
        private const val ARG_PHOTO_PATH = "photoPath"
    }
}