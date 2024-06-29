/*
 * Copyright (c) 2018 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish, 
 * distribute, sublicense, create a derivative work, and/or sell copies of the 
 * Software in any work that is designed, intended, or marketed for pedagogical or 
 * instructional purposes related to programming, coding, application development, 
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works, 
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.earl.collageapp

import androidx.lifecycle.ViewModel
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit


class SharedViewModel : ViewModel() {

    private val disposables = CompositeDisposable()
    //Emitira los valores para lista de photos
    private val imagesSubject: BehaviorSubject<MutableList<Photo>> = BehaviorSubject.createDefault(
        mutableListOf()
    )

    // Usamos esto para mostrar las fotos en el MainActivity
    private val selectedPhotos = MutableLiveData<List<Photo>>()
    private val thumbnailStatus = MutableLiveData<ThumbnailStatus>()
    private val collageStatus = MutableLiveData<CollageStatus>()

    init {
        /*Gracias a esto cada que agregamos un onNext event (hacemos click en algun boton) el imagesSubject
          le agrega lo que contiene a nuestra lista selectedPhotos  */
        imagesSubject.subscribe { listOfPhotos ->
            //Recordar que para acceder al valor real(en este caso lista) siempre tenemos que usar value, ej.
            // selectedPhotos.value o podemos usar una delegate properties como by
            selectedPhotos.value = listOfPhotos
        }.addTo(disposables)
    }

    //Cuando el activity deja de existir dejamos de recibir eventos
    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
//
//    fun addPhoto(photo: Photo){
//        // Ya que el imagesSubject recibe una lista podemos usar los metodos de mutableList (add, remove)
//        imagesSubject.value?.add(photo)
//        //Aqui agregamos toda la lista al imagesSubject
//        imagesSubject.onNext(imagesSubject.value!!)
//    }

    fun clearPhotos() {
        imagesSubject.value?.clear()
        //Esto hace que se actualice nuestra photosSelected
        imagesSubject.onNext(imagesSubject.value!!)
    }

    fun subscribeSelectedPhotos(fragment: PhotosBottomDialogFragment) {
        val newPhotos = fragment.selectedPhotos.share()

        //Cada vez que nos subscribimos a un observable se crea una nuevo Observable y esto no garantiza que la copia
        // sea la misma que la anterior segun el libro con share permites que multiples subscripciones
        //consuman los elementos que un solo observable produce para ellos.
        //Segun dice que no se debe de usar con observables que nunca se completan,
        // o si garantizas que ninguna subscription se hara despues del onComplete
        disposables.add(newPhotos
            .doOnComplete {
                Log.v("SharedViewModel", "Completed selecting photos")
            }.takeWhile{
                //Descarta todos los elementos cuando esta condicion se vuelve falsa
                (imagesSubject.value?.size ?: 0) < 6
            }.filter{ newImage ->
                val bitmap = BitmapFactory.decodeResource(
                    fragment.resources, newImage.drawable)
                bitmap.width > bitmap.height
            }.filter{ newImage ->
                //Si cumple dejalo pasar
                val photos = imagesSubject.value ?: mutableListOf()
                //One way of achive the same result
//                var bool = photos.size < 1
//                photos.forEach { photo ->
//                    bool = photo.drawable != newImage.drawable
//                }
//                bool
                //Llamamos map en la lista de photos para transformarlo en una lista de Int, y comparamos la list
                // para ver si contiene el drawable que seleccionamos
                !(photos.map { it.drawable }).contains(newImage.drawable)
                //debounce limita el numero de eventos que pasan al subscribe, toma un limite de tiempo ,
                // y se asegura que ningun elemento sea emitido hasta que ese tiempo pase
            }.debounce(250, TimeUnit.MILLISECONDS,
                AndroidSchedulers.mainThread()
            ).subscribe { photo ->
                imagesSubject.value?.add(photo)
                imagesSubject.onNext(imagesSubject.value ?:
                mutableListOf())
            }
        )

        disposables.add(newPhotos.ignoreElements().subscribe{
            thumbnailStatus.postValue(ThumbnailStatus.READY)
        })

        disposables.add(newPhotos.skipWhile{
            (imagesSubject.value?.size ?: 0) < 6
        }.subscribe{
            collageStatus.postValue(CollageStatus.LIMIT_REACHED)
        })

//        disposables.add()

        //Como tenemos los eventos(elementos) que emitio nuestro publishSubject en el Fragment entonces
        // lo que hacemos es para cada evento emitido
//        newPhotos.doOnComplete {
//            Log.v("SharedViewModel", "Completed selecting photos")
//        }
//            /*Cada vez que se da click en alguna imagen de nuestro recyclerView como tenemos una copia,
//               del observable y lo estamos subscribiendo (observando nuestro publishSubject)
//            * */
//            .subscribe { photo ->
//                Log.v("SharedViewModel", "subscribeSelectedPhotos: $newPhotos")
//                imagesSubject.value?.add(photo)
//                imagesSubject.onNext(imagesSubject.value!!)
//            }
//            .addTo(disposables)
    }

    fun getSelectedPhotos(): LiveData<List<Photo>> {
        return selectedPhotos
    }

    fun saveBitmapFromImageView(imageView: ImageView, context: Context): Single<String> {
        return Single.create { emitter ->
            val tmpImg = "${System.currentTimeMillis()}.png"

            val os: OutputStream?

            val collagesDirectory = File(context.getExternalFilesDir(null), "collages")
            if (!collagesDirectory.exists()) {
                collagesDirectory.mkdirs()
            }

            val file = File(collagesDirectory, tmpImg)

            try {
                os = FileOutputStream(file)
                val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                os.flush()
                os.close()
                emitter.onSuccess(tmpImg)
            } catch (e: IOException) {
                Log.e("MainActivity", "Problem saving collage", e)
                emitter.onError(e)
            }
        }
    }

    enum class ThumbnailStatus{
        READY,
        ERROR
    }

    fun getThumbnailStatus() : LiveData<ThumbnailStatus>{
        return thumbnailStatus
    }

    enum class CollageStatus{
        OK,
        LIMIT_REACHED
    }

    fun getCollageStatus(): LiveData<CollageStatus>{
        return collageStatus
    }
}