package com.earl.collageapp

object PhotoStore {
    val photos: List<Photo> by lazy {
        val photos = mutableListOf<Photo>()
        photos.add(Photo(R.drawable.image1))
        photos.add(Photo(R.drawable.image2))
        photos.add(Photo(R.drawable.image3))
        photos.add(Photo(R.drawable.image4))
        photos.add(Photo(R.drawable.image5))
        photos.add(Photo(R.drawable.image6))
        photos.add(Photo(R.drawable.image7))
        photos.add(Photo(R.drawable.image8))
        photos.add(Photo(R.drawable.image9))
        photos.add(Photo(R.drawable.image10))
        photos.add(Photo(R.drawable.image11))
        photos.add(Photo(R.drawable.image12))
        photos.add(Photo(R.drawable.image13))
        photos
    }

}