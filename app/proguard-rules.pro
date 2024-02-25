-optimizationpasses 8
-dontobfuscate
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void checkExpressionValueIsNotNull(...);
	public static void checkNotNullExpressionValue(...);
	public static void checkReturnedValueIsNotNull(...);
	public static void checkFieldIsNotNull(...);
	public static void checkParameterIsNotNull(...);
	public static void checkNotNullParameter(...);
}
-keep public class ** extends com.example.dexreader.core.ui.BaseFragment
-keep class com.example.dexreader.core.db.entity.* { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class com.example.dexreader.core.exceptions.* { *; }
-keep class com.example.dexreader.settings.NotificationSettingsLegacyFragment
-keep class com.example.dexreader.core.prefs.ScreenshotsPolicy { *; }
-keep class org.jsoup.parser.Tag
-keep class org.jsoup.internal.StringUtil
