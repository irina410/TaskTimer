package com.example.tasktimer.model

import android.os.Parcel
import android.os.Parcelable

data class Subtask(
    var description: String,
    var duration: Long,
    var isHighPriority: Boolean = false,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        description = parcel.readString() ?: "",
        duration = parcel.readLong(),
        isHighPriority = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(description)
        parcel.writeLong(duration)
        parcel.writeByte(if (isHighPriority) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Subtask> {
        override fun createFromParcel(parcel: Parcel): Subtask {
            return Subtask(parcel)
        }

        override fun newArray(size: Int): Array<Subtask?> {
            return arrayOfNulls(size)
        }
    }
}