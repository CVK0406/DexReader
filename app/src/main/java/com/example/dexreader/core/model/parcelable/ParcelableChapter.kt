package com.example.dexreader.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.example.dexreader.core.util.ext.readSerializableCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.example.dexreader.parsers.model.MangaChapter
import org.example.dexreader.parsers.model.MangaSource

@Parcelize
data class ParcelableChapter(
	val chapter: MangaChapter,
) : Parcelable {

	companion object : Parceler<ParcelableChapter> {

		override fun create(parcel: Parcel) = ParcelableChapter(
			MangaChapter(
				id = parcel.readLong(),
				name = parcel.readString().orEmpty(),
				number = parcel.readFloat(),
				volume = parcel.readInt(),
				url = parcel.readString().orEmpty(),
				scanlator = parcel.readString(),
				uploadDate = parcel.readLong(),
				branch = parcel.readString(),
				source = parcel.readSerializableCompat() as? MangaSource ?: MangaSource.DUMMY
			)
		)

		override fun ParcelableChapter.write(parcel: Parcel, flags: Int) = with(chapter) {
			parcel.writeLong(id)
			parcel.writeString(name)
			parcel.writeFloat(number)
			parcel.writeInt(volume)
			parcel.writeString(url)
			parcel.writeString(scanlator)
			parcel.writeLong(uploadDate)
			parcel.writeString(branch)
			parcel.writeSerializable(source)
		}
	}
}
