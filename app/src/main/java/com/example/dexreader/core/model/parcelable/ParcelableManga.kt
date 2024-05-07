package com.example.dexreader.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.example.dexreader.core.util.ext.readParcelableCompat
import com.example.dexreader.core.util.ext.readSerializableCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.example.dexreader.parsers.model.Manga

@Parcelize
data class ParcelableManga(
	val manga: Manga,
) : Parcelable {

	companion object : Parceler<ParcelableManga> {

		override fun ParcelableManga.write(parcel: Parcel, flags: Int) = with(manga) {
			parcel.writeLong(id)
			parcel.writeString(title)
			parcel.writeString(altTitle)
			parcel.writeString(url)
			parcel.writeString(publicUrl)
			parcel.writeFloat(rating)
			ParcelCompat.writeBoolean(parcel, isNsfw)
			parcel.writeString(coverUrl)
			parcel.writeString(largeCoverUrl)
			parcel.writeString(description)
			parcel.writeParcelable(ParcelableMangaTags(tags), flags)
			parcel.writeSerializable(state)
			parcel.writeString(author)
			parcel.writeSerializable(source)
		}

		override fun create(parcel: Parcel) = ParcelableManga(
			Manga(
				id = parcel.readLong(),
				title = requireNotNull(parcel.readString()),
				altTitle = parcel.readString(),
				url = requireNotNull(parcel.readString()),
				publicUrl = requireNotNull(parcel.readString()),
				rating = parcel.readFloat(),
				isNsfw = ParcelCompat.readBoolean(parcel),
				coverUrl = requireNotNull(parcel.readString()),
				largeCoverUrl = parcel.readString(),
				description = parcel.readString(),
				tags = requireNotNull(parcel.readParcelableCompat<ParcelableMangaTags>()).tags,
				state = parcel.readSerializableCompat(),
				author = parcel.readString(),
				chapters = null,
				source = requireNotNull(parcel.readSerializableCompat()),
			)
		)
	}
}
