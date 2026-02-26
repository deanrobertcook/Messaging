package com.android.messaging.ui.conversationlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.view.LayoutInflater
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
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationListData.ConversationListDataListener
import android.database.Cursor
import android.util.Log

class ConversationListActivity : ComponentActivity(), ConversationListDataListener {
    private val mListBinding: Binding<ConversationListData> = BindingBase.createBinding(this);
    private val mArchiveMode = false

    override fun onConversationListCursorUpdated(data: ConversationListData, cursor: Cursor) {
        Log.d("TEST", "onConversationListCursorUpdated " + cursor.getColumnCount())
    }

    override fun setBlockedParticipantsAvailable(blockedAvailable: Boolean) {
        Log.d("TEST", "onConversationListCursorUpdated")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mListBinding.bind(DataModel.get().createConversationListData(this, this, mArchiveMode));
        mListBinding.getData().init(LoaderManager.getInstance(this), mListBinding);
        setContent {
            ComposeTutorialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MessageCard(
                        Message("Android", "Jetpack Compose")
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mListBinding.unbind()
    }
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    Text(text = "hello")
    // AndroidView({ context ->
    //     LayoutInflater.from(context).inflate(
    //         R.layout.conversation_list_item_view, null, false
    //     )
    // })
}

