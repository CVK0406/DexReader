package com.example.dexreader.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.example.dexreader.core.util.ext.readSerializableCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.example.dexreader.parsers.model.MangaPage

object MangaPageParceler : Parceler<MangaPage> {
	override fun create(parcel: Parcel) = MangaPage(
		id = parcel.readLong(),
		url = requireNotNull(parcel.readString()),
		preview = parcel.readString(),
		source = requireNotNull(parcel.readSerializableCompat()),
	)

	override fun MangaPage.write(parcel: Parcel, flags: Int) {
		parcel.writeLong(id)
		parcel.writeString(url)
		parcel.writeString(preview)
		parcel.writeSerializable(source)
	}
}

@Parcelize
@TypeParceler<MangaPage, MangaPageParceler>
class ParcelableMangaPage(val page: MangaPage) : Parcelable
