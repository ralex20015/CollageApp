package com.earl.collageapp

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.earl.collageapp.databinding.ActivityMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //this changes the title of the appbar
        binding.myToolbar.setTitle(R.string.collage)

        binding.addButton.setOnClickListener {
            actionAdd()
        }

        binding.clearButton.setOnClickListener {
            actionClear()
        }

        binding.saveButton.setOnClickListener {
            actionSave()
        }
        //Another way to initialize a viewmodel
        //sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        //observamos el selectedPhotos live data que tenemos en el sharedViewModel que emite una lista de Photo
        //Esto actualiza la UI
        sharedViewModel.getSelectedPhotos().observe(this) { photos ->
            photos?.let {
                //Checamos que no este vacia la lista para empezar a agregar al ImageView collageImage
                if (photos.isNotEmpty()) {
                    //Si hay alguna foto transformamos cada Photo a un Bitmap
                    val bitmaps = photos.map { photo ->
                        BitmapFactory.decodeResource(resources, photo.drawable)
                    }
                    //Creamos un Bitmap combinando las imagenes que tenemos en nuestra lista selectedPhotos
                    val newBitmap = combineImages(bitmaps)
                    //Establecemos el valor de nuestra ImageView del collage con el newBitmap que contiene,
                    // todas las imagenes ya transformadas en un Bitmap
                    binding.collageImage.setImageDrawable(
                        BitmapDrawable(resources, newBitmap)
                    )
                } else {
                    //La lista esta vacia por lo tanto tenemos que definir que queremos hacer cuando este vacia
                    // en este caso pues damos un color a la imagen transparente
                    binding.collageImage.setImageResource(android.R.color.transparent)
                }
                updateUI(it)
            }
        }

        sharedViewModel.getThumbnailStatus().observe(this) { status ->
            if (status == SharedViewModel.ThumbnailStatus.READY) {
                binding.thumbnail.setImageDrawable(binding.collageImage.drawable)
            }
        }

        sharedViewModel.getCollageStatus().observe(this) { status ->
            if (status == SharedViewModel.CollageStatus.LIMIT_REACHED) {
                Toast.makeText(this, "Alcanzaste el limite de fotos!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(photos: List<Photo>) {
        binding.saveButton.isEnabled =
            photos.isNotEmpty() && (photos.size % 2 == 0)
        binding.clearButton.isEnabled = photos.isNotEmpty()
        binding.addButton.isEnabled = photos.size < 6

        binding.myToolbar.title = if (photos.isNotEmpty()) {
            resources.getQuantityString(R.plurals.photos_format, photos.size, photos.size)
        } else {
            getString(R.string.collage)
        }
    }

    private fun actionAdd() {
        val addPhotoBottomDialogFragment =
            PhotosBottomDialogFragment.newInstance()
        addPhotoBottomDialogFragment.show(supportFragmentManager, "PhotosBottomDialogFragment")
        //En este metodo del viewModel nos subscribimos a nuestro observable selectedPhotos del PhotosBottomDialogFragment
        // Por lo tanto cada vez que se emite un evento en el Fragment como nuestro behaviorSubject, nos trae el ultimo
        // evento pues se observa en la UI
        sharedViewModel.subscribeSelectedPhotos(addPhotoBottomDialogFragment)
    }

    private fun actionClear() {
        sharedViewModel.clearPhotos()
    }

    @SuppressLint("CheckResult")
    private fun actionSave() {
        sharedViewModel.saveBitmapFromImageView(binding.collageImage, this)
            .subscribeOn(Schedulers.io()) //Le dice al single que haga la subscripcion en el IO Scheduler
            .observeOn(AndroidSchedulers.mainThread()) //Le dice a al single que corra el subscribeBy en hilo principal de android
            .subscribeBy(
                onSuccess = { file ->
                    Toast.makeText(
                        this, "$file saved",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = {
                    Toast.makeText(
                        this,
                        "Error saving file :${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
    }
}