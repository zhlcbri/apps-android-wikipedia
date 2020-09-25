package org.wikipedia.feed.suggestededits

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.*
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.headerView
import kotlinx.android.synthetic.main.view_suggested_edits_cards.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    private var card: SuggestedEditsCard? = null
    private var view: View

    init {
        view = inflate(getContext(), R.layout.view_suggested_edits_cards, this)
    }

    override fun setCard(card: SuggestedEditsCard) {
        super.setCard(card)
        this.card = card

        if (card.sourceSummaryForEdit != null) {
            setLayoutDirectionByWikiSite(WikiSite.forLanguageCode(card.sourceSummaryForEdit.lang), this)
        }
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        setUpPagerWithSECards()
    }

    private fun setUpPagerWithSECards() {
        seCardsPager.adapter = SECardsPagerAdapter(view.context as AppCompatActivity)
        TabLayoutMediator(seCardsIndicatorLayout, seCardsPager)
        { tab: TabLayout.Tab, position: Int -> tab.view.isClickable = false }.attach()
    }

    class SECardsPagerAdapter(activity: AppCompatActivity?) : PositionAwareFragmentStateAdapter(activity!!) {
        private val seCardTypeList = ArrayList<DescriptionEditActivity.Action>()

        init {
            seCardTypeList.add(ADD_DESCRIPTION)
            seCardTypeList.add(ADD_CAPTION)
            seCardTypeList.add(ADD_IMAGE_TAGS)
        }

        override fun getItemCount(): Int {
            return seCardTypeList.size
        }

        override fun createFragment(position: Int): Fragment {
            return SuggestedEditsCardItemFragment.newInstance("", "")
        }
    }

    private fun showImageTagsUI() {
        viewArticleImage.visibility = View.VISIBLE
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        viewArticleImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(card!!.page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
        viewArticleTitle.visibility = View.GONE
        callToActionText.text = context.getString(R.string.suggested_edits_feed_card_add_image_tags)
    }

    private fun showAddDescriptionUI() {
        viewArticleTitle.visibility = View.VISIBLE
        viewArticleTitle.text = StringUtil.fromHtml(card!!.sourceSummaryForEdit!!.displayTitle!!)
        callToActionText.text = if (card!!.action == TRANSLATE_DESCRIPTION) context.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button, app.language().getAppLanguageCanonicalName(card!!.targetSummaryForEdit!!.lang)) else context.getString(R.string.suggested_edits_feed_card_add_description_button)
        showImageOrExtract()
    }

    private fun showTranslateDescriptionUI() {
        viewArticleTitle.visibility = View.VISIBLE
        sourceDescription = card!!.sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddDescriptionUI()
    }

    private fun showAddImageCaptionUI() {
        viewArticleTitle.visibility = View.VISIBLE
        viewArticleImage.visibility = View.VISIBLE
        viewArticleExtract.visibility = View.GONE
        divider.visibility = View.GONE
        viewArticleImage.loadImage(Uri.parse(card!!.sourceSummaryForEdit!!.thumbnailUrl))
        viewArticleTitle.text = StringUtil.removeNamespace(card!!.sourceSummaryForEdit!!.displayTitle!!)
        callToActionText.text = if (card!!.action == TRANSLATE_CAPTION) context.getString(R.string.suggested_edits_feed_card_translate_image_caption, app.language().getAppLanguageCanonicalName(card!!.targetSummaryForEdit!!.lang)) else context.getString(R.string.suggested_edits_feed_card_add_image_caption)
    }

    private fun showTranslateImageCaptionUI() {
        viewArticleTitle.visibility = View.VISIBLE
        sourceDescription = card!!.sourceSummaryForEdit!!.description!!
        viewArticleSubtitle.visibility = View.VISIBLE
        viewArticleSubtitle.text = sourceDescription
        showAddImageCaptionUI()
    }

    private fun showImageOrExtract() {
        if (card!!.sourceSummaryForEdit!!.thumbnailUrl.isNullOrBlank()) {
            viewArticleImage.visibility = View.GONE
            viewArticleExtract.visibility = View.VISIBLE
            divider.visibility = View.VISIBLE
            viewArticleExtract.text = StringUtil.fromHtml(card!!.sourceSummaryForEdit!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = View.VISIBLE
            viewArticleExtract.visibility = View.GONE
            divider.visibility = View.GONE
            viewArticleImage.loadImage(Uri.parse(card!!.sourceSummaryForEdit!!.thumbnailUrl))
        }
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setLangCode(if (card.action == TRANSLATE_CAPTION || card.action == TRANSLATE_DESCRIPTION) card.targetSummaryForEdit!!.lang else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun refreshCardContent() {
        SuggestedEditsFeedClient(card!!.action).fetchSuggestedEditForType(null, this)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 6
    }
}
