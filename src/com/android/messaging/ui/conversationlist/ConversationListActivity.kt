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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.android.messaging.ui.UIIntents
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class ConversationListActivity : ComponentActivity(), ConversationListDataListener, HostInterface {
    private val mListBinding: Binding<ConversationListData> = BindingBase.createBinding(this)
    private val mArchiveMode = false

    private val viewModel: ConversationListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mListBinding.bind(DataModel.get().createConversationListData(this, this, mArchiveMode))
        mListBinding.getData().init(LoaderManager.getInstance(this), mListBinding)

        setContent {
            ConversationList(viewModel, this, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mListBinding.unbind()
    }

    /**
     * ConversationListDataListener
     **/
    override fun onConversationListCursorUpdated(data: ConversationListData, cursor: Cursor) {
        Log.d(LogUtil.BUGLE_TAG, "onConversationListCursorUpdated " + cursor.getCount())
        val listItems = mutableListOf<ConversationListItemData>()
        while (cursor.moveToNext()) {
            listItems.add(ConversationListItemData().apply { this.bind(cursor) })
        }
        viewModel.onItemsLoaded(listItems)
    }

    override fun setBlockedParticipantsAvailable(blockedAvailable: Boolean) {
        Log.d(LogUtil.BUGLE_TAG, "setBlockedParticipantsAvailable: " + blockedAvailable)
    }

    /**
     * ConversationListItemView.HostInterface
     **/
    override fun isConversationSelected(conversationId: String): Boolean {
        return viewModel.uiState.value.selectedIds.contains(conversationId)
    }

    override fun onConversationClicked(
        conversationListItemData: ConversationListItemData,
        isLongClick: Boolean,
        conversationView: ConversationListItemView
    ) {
        val conversationId = conversationListItemData.getConversationId()
        val selectedIds = viewModel.uiState.value.selectedIds
        if (selectedIds.isEmpty() && !isLongClick) {
            UIIntents.get().launchConversationActivity(this, conversationId, null, null, false)
        } else {
            viewModel.onClick(conversationId)
        }
    }

    override fun isSwipeAnimatable(): Boolean {
        return false
    }

    override fun getSnackBarInteractions(): List<SnackBarInteraction> {
        return emptyList<SnackBarInteraction>()
    }

    override fun startFullScreenPhotoViewer(initialPhoto: Uri, initialPhotoBounds: Rect, photosUri: Uri) {
        UIIntents.get().launchFullScreenPhotoViewer(
            this, initialPhoto, initialPhotoBounds, photosUri
        )
    }

    override fun startFullScreenVideoViewer(videoUri: Uri) {
        UIIntents.get().launchFullScreenVideoViewer(this, videoUri)
    }

    override fun isSelectionMode(): Boolean {
        return !viewModel.uiState.value.selectedIds.isEmpty()
    }
}

class ConversationListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListState())
    val uiState: StateFlow<ConversationListState> = _uiState.asStateFlow()

    fun onItemsLoaded(items: List<ConversationListItemData>) {
        _uiState.update { it.copy(items = items) }
    }

    fun onClick(conversationId: String) {
        _uiState.update { state ->
            val newSelected = if (conversationId in state.selectedIds) {
                state.selectedIds - conversationId
            } else {
                state.selectedIds + conversationId
            }
            state.copy(selectedIds = newSelected)
        }
    }
}

data class ConversationListState(
    val items: List<ConversationListItemData> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationList(
    viewModel: ConversationListViewModel = viewModel(),
    hostInterface: HostInterface,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(uiState.items, key = { it.getConversationId() }) { listItem ->
                    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == EndToStart) {
                                UpdateConversationArchiveStatusAction.archiveConversation(listItem.getConversationId())
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "1 archived",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        UpdateConversationArchiveStatusAction.unarchiveConversation(listItem.getConversationId())
                                    }
                                }
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
                                LayoutInflater.from(context).inflate(
                                    R.layout.conversation_list_item_view, null, false
                                ) as ConversationListItemView
                            },
                            update = { view ->
                                val isSelected = uiState.selectedIds.contains(listItem.getConversationId())
                                val isSelectionMode = !uiState.selectedIds.isEmpty()
                                view.bind(listItem, hostInterface, isSelected, isSelectionMode)
                            }
                        )
                    }
                }
            }
        }
    }
}

