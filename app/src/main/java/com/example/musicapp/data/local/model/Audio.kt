package com.example.musicapp.data.local.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.net.toUri

data class Audio(
    val uri: Uri,
    val displayName: String,
    val id: Long,
    val artist: String,
    val data: String,
    val duration: Int,
    val title: String
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()?.toUri() ?: "".toUri(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString().toString(),
        parcel.readString() ?: "",
        parcel.readLong().toInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uri.toString())
        parcel.writeString(displayName)
        parcel.writeLong(id)
        parcel.writeString(artist)
        parcel.writeString(data)
        parcel.writeLong(duration.toLong())
        parcel.writeString(title)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Audio> {
        override fun createFromParcel(parcel: Parcel): Audio {
            return Audio(parcel)
        }

        override fun newArray(size: Int): Array<Audio?> {
            return arrayOfNulls(size)
        }
    }
}