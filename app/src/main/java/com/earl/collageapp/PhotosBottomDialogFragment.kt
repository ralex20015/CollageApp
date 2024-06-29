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

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
//import androidx.lifecycle.ViewModelProvider // we need this for the other form of initialize viewmodel
import com.earl.collageapp.databinding.LayoutPhotoBottomSheetBinding
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject


class PhotosBottomDialogFragment : BottomSheetDialogFragment(), PhotosAdapter.PhotoListener {

  private var _binding: LayoutPhotoBottomSheetBinding? = null
  private val binding: LayoutPhotoBottomSheetBinding
    get() = checkNotNull(_binding){
      "Cannot access binding because it is null. Is the view visible?"
    }

  //this seems like flows
  //Creamos un publishSubject pero no queremos exponerlo a las demas clases porque queremos saber que es lo que se pone
  //dentro de este subject
  private val selectedPhotosSubject = PublishSubject.create<Photo>()

  //El metodo hide unicamente retorna una version Observable del mismo Subject (solo podemos observar pero no agregar
  // nuevos eventos unicamente definimos que es lo que vamos a hacer cuando recibamos un nuevo evento
  val selectedPhotos: Observable<Photo>
    get() = selectedPhotosSubject.hide()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = LayoutPhotoBottomSheetBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //One way to initialize a viewmodel
//    val ctx = activity
//    ctx?.let {
//      viewModel = ViewModelProvider(this)[SharedViewModel::class.java]
//    }

    val recyclerView = binding.photosRecyclerViewOfCollage
    recyclerView.layoutManager = GridLayoutManager(context, 3)
    recyclerView.adapter = PhotosAdapter(PhotoStore.photos, this)
  }

  override fun onDestroyView() {
    //IMPORTANTE tenemos que por decirlo asi destruir nuestro subject, porque si no seguira en memoria y
    // cuando volvamos a abrir el fragment se creara otro subject
    selectedPhotosSubject.onComplete()
    _binding = null
    super.onDestroyView()
  }

  override fun photoClicked(photo: Photo) {
    selectedPhotosSubject.onNext(photo)
  }

  companion object {
    fun newInstance(): PhotosBottomDialogFragment {
      return PhotosBottomDialogFragment()
    }
  }
}