package com.android.messaging.ui.conversationlist

import android.os.Bundle
import android.content.Context
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
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.android.messaging.ui.theme.MainTheme
import com.android.messaging.ui.ContactIconView
import com.android.messaging.R

import com.android.messaging.datamodel.binding.Binding
import com.android.messaging.datamodel.binding.BindingBase
import androidx.loader.app.LoaderManager
import com.android.messaging.datamodel.data.ConversationListData
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationListData.ConversationListDataListener
import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction
import com.android.messaging.ui.conversationlist.ConversationListItemView.HostInterface
import com.android.messaging.ui.SnackBarInteraction
import android.database.Cursor
import android.util.Log
import com.android.messaging.util.LogUtil
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.Settled
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.android.messaging.ui.UIIntents
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

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
        if (!cursor.moveToFirst()) {
            // TODO: empty list
            return
        }

        val listItems = mutableListOf<ConversationListItemData>()
        do {
            listItems.add(ConversationListItemData().apply { this.bind(cursor) })
        } while (cursor.moveToNext())

        setContent {
            ConversationList(listItems, this, this)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationList(listItems: List<ConversationListItemData>, hostInterface: HostInterface, context: Context) {
    MainTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Messaging")
                    },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archived") },
                                onClick = {
                                    UIIntents.get().launchArchivedConversationsActivity(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    UIIntents.get().launchSettingsActivity(context)
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    UIIntents.get().launchCreateNewConversationActivity(context, null);
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(listItems, key = { it.getConversationId() }) { listItem ->
                    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == EndToStart) {
                                UpdateConversationArchiveStatusAction.archiveConversation(listItem.getConversationId())
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = swipeToDismissBoxState,
                        backgroundContent = {
                            when (swipeToDismissBoxState.dismissDirection) {
                                StartToEnd -> {}
                                EndToStart -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = "Archive"
                                        )
                                    }
                                }

                                Settled -> {}
                            }
                        },
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize(),
                            factory = { context ->
                                val view = LayoutInflater.from(context).inflate(
                                    R.layout.conversation_list_item_view, null, false
                                ) as ConversationListItemView
                                view.bind(listItem, hostInterface)
                                view
                            }
                        )
                    }
                }
            }
        }
    }
}

