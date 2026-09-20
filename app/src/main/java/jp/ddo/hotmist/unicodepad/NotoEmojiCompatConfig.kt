/*
   Copyright 2018 Ryosuke839

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package jp.ddo.hotmist.unicodepad

import android.content.Context
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.MetadataRepo

private const val NOTO_EMOJI_COMPAT_ASSET = "NotoColorEmoji-emojicompat.ttf"

internal class NotoEmojiCompatConfig(context: Context) : EmojiCompat.Config(
    AssetMetadataLoader(context.applicationContext)
)

private class AssetMetadataLoader(
    private val context: Context,
) : EmojiCompat.MetadataRepoLoader {
    override fun load(loaderCallback: EmojiCompat.MetadataRepoLoaderCallback) {
        Thread {
            try {
                loaderCallback.onLoaded(MetadataRepo.create(context.assets, NOTO_EMOJI_COMPAT_ASSET))
            } catch (t: Throwable) {
                loaderCallback.onFailed(t)
            }
        }.start()
    }
}
