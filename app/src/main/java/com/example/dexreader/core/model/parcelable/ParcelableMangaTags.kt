package com.example.dexreader.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.example.dexreader.core.util.ext.readSerializableCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.example.dexreader.parsers.model.MangaTag

object MangaTagParceler : Parceler<MangaTag> {
	override fun create(parcel: Parcel) = MangaTag(
		title = requireNotNull(parcel.readString()),
		key = requireNotNull(parcel.readString()),
		source = requireNotNull(parcel.readSerializableCompat()),
	)

	override fun MangaTag.write(parcel: Parcel, flags: Int) {
		parcel.writeString(title)
		parcel.writeString(key)
		parcel.writeSerializable(source)
	}
}

@Parcelize
@TypeParceler<MangaTag, MangaTagParceler>
data class ParcelableMangaTags(val tags: Set<MangaTag>) : Parcelable
