package io.github.droidkaigi.confsched2018.presentation.sessions.item

import android.support.v4.app.Fragment
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.xwray.groupie.databinding.BindableItem
import io.github.droidkaigi.confsched2018.R
import io.github.droidkaigi.confsched2018.databinding.ItemSpeechSessionBinding
import io.github.droidkaigi.confsched2018.model.Session
import io.github.droidkaigi.confsched2018.util.CustomGlideApp
import io.github.droidkaigi.confsched2018.util.ext.toGone
import io.github.droidkaigi.confsched2018.util.ext.toVisible
import io.github.droidkaigi.confsched2018.util.lang
import android.text.Spanned
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import java.util.regex.Pattern
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan


data class SpeechSessionItem(
        override val session: Session.SpeechSession,
        private val onFavoriteClickListener: (Session.SpeechSession) -> Unit,
        private val fragment: Fragment,
        private val isShowDayNumber: Boolean = false,
        private val highlightedString: String? = null
) : BindableItem<ItemSpeechSessionBinding>(
        session.id.toLong()
), SessionItem {

    override fun bind(viewBinding: ItemSpeechSessionBinding, position: Int) {
        viewBinding.session = session

        if (highlightedString != null) {
            viewBinding.title.text = adaptHighlightString(session.title, highlightedString)
            viewBinding.description.text = adaptHighlightString(session.desc, highlightedString)
        } else {
            viewBinding.title.text = session.title
            viewBinding.description.text = session.desc
        }

        viewBinding.topic.text = session.topic.name
        viewBinding.level.text = session.level.getNameByLang(lang())
        val speakerImages = arrayOf(
                viewBinding.speakerImage1,
                viewBinding.speakerImage2,
                viewBinding.speakerImage3,
                viewBinding.speakerImage4,
                viewBinding.speakerImage5
        )
        speakerImages.forEachIndexed { index, imageView ->
            if (index < session.speakers.size) {
                imageView.toVisible()
                val size = viewBinding.root.resources.getDimensionPixelSize(R.dimen.speaker_image)
                CustomGlideApp
                        .with(fragment)
                        .load(session.speakers[index].imageUrl)
                        .placeholder(R.drawable.ic_person_black_24dp)
                        .override(size, size)
                        .dontAnimate()
                        .transform(CircleCrop())
                        .into(imageView)
            } else {
                imageView.toGone()
            }
        }

        viewBinding.speakers.text = session.speakers.joinToString { it.name }
        viewBinding.favorite.setOnClickListener {
            onFavoriteClickListener(session)
        }
        viewBinding.isShowDayNumber = isShowDayNumber
    }

    private fun adaptHighlightString(message: String, highlightString: String): SpannableString {
        val ss = SpannableString(message)

        val pattern = Pattern.compile(highlightString, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(message)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            ss.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss.setSpan(BackgroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return ss
    }


    override fun getLayout(): Int = R.layout.item_speech_session
}
