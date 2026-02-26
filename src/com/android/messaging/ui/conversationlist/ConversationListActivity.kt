package com.android.messaging.ui.conversationlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.view.LayoutInflater
import android.net.Uri
import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.android.messaging.ui.theme.ComposeTutorialTheme
import com.android.messaging.ui.ContactIconView
import com.android.messaging.R

import com.android.messaging.datamodel.binding.Binding
import com.android.messaging.datamodel.binding.BindingBase
import androidx.loader.app.LoaderManager
import com.android.messaging.datamodel.data.ConversationListData
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationListData.ConversationListDataListener
import com.android.messaging.ui.conversationlist.ConversationListItemView.HostInterface;
import com.android.messaging.ui.SnackBarInteraction
import android.database.Cursor
import android.util.Log
import com.android.messaging.util.LogUtil
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class ConversationListActivity : ComponentActivity(), ConversationListDataListener, HostInterface {
    private val mListBinding: Binding<ConversationListData> = BindingBase.createBinding(this);
    private val mArchiveMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mListBinding.bind(DataModel.get().createConversationListData(this, this, mArchiveMode))
        mListBinding.getData().init(LoaderManager.getInstance(this), mListBinding)
    }

    override fun onDestroy() {
        super.onDestroy()
        mListBinding.unbind()
    }

    /**
     * ConversationListDataListener
     **/
    override fun onConversationListCursorUpdated(data: ConversationListData, cursor: Cursor) {
        Log.d(
            LogUtil.BUGLE_TAG,
            "onConversationListCursorUpdated " + cursor.getColumnCount() + ": " + cursor.getCount()
        )
        if (!cursor.moveToFirst()) {
            // TODO: empty list
            return
        }

        val listItems = mutableListOf<ConversationListItemData>()
        do {
            listItems.add(ConversationListItemData().apply { this.bind(cursor) })
        } while (cursor.moveToNext())

        setContent {
            ComposeTutorialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn {
                        items(listItems) { listItem ->
                            AndroidView({ context ->
                                val view = LayoutInflater.from(context).inflate(
                                    R.layout.conversation_list_item_view, null, false
                                ) as ConversationListItemView
                                view.bind(listItem, this@ConversationListActivity)
                                view
                            })

                        }
                    }
                }
            }
        }
    }

    override fun setBlockedParticipantsAvailable(blockedAvailable: Boolean) {
        Log.d(LogUtil.BUGLE_TAG, "setBlockedParticipantsAvailable: " + blockedAvailable)
    }

    /**
     * ConversationListItemView.HostInterface
     **/
    override fun isConversationSelected(conversationId: String): Boolean {
        Log.d(LogUtil.BUGLE_TAG, "isConversationSelected")
        return false
    }

    override fun onConversationClicked(
        conversationListItemData: ConversationListItemData?,
        isLongClick: Boolean,
        conversationView: ConversationListItemView
    ) {
        Log.d(LogUtil.BUGLE_TAG, "onConversationClicked")
    }

    override fun isSwipeAnimatable(): Boolean {
        Log.d(LogUtil.BUGLE_TAG, "isSwipeAnimatable")
        return false
    }

    override fun getSnackBarInteractions(): List<SnackBarInteraction> {
        Log.d(LogUtil.BUGLE_TAG, "getSnackBarInteractions")
        return emptyList<SnackBarInteraction>()
    }

    override fun startFullScreenPhotoViewer(initialPhoto: Uri, initialPhotoBounds: Rect, photosUri: Uri) {
        Log.d(LogUtil.BUGLE_TAG, "startFullScreenPhotoViewer")
    }

    override fun startFullScreenVideoViewer(videoUri: Uri) {
        Log.d(LogUtil.BUGLE_TAG, "startFullScreenVideoViewer")
    }

    override fun isSelectionMode(): Boolean {
        Log.d(LogUtil.BUGLE_TAG, "isSelectionMode")
        return false
    }
}

