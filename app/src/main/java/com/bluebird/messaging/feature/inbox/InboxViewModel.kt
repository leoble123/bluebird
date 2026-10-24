package com.bluebird.messaging.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bluebird.messaging.core.model.Conversation
import com.bluebird.messaging.core.provider.MessagingProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InboxUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val isSimulated: Boolean = true,
)

class InboxViewModel(provider: MessagingProvider) : ViewModel() {

    val uiState: StateFlow<InboxUiState> = provider.observeConversations()
        .map { InboxUiState(isLoading = false, conversations = it, isSimulated = provider.info.isSimulated) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    companion object {
        fun factory(provider: MessagingProvider): ViewModelProvider.Factory = viewModelFactory {
            initializer { InboxViewModel(provider) }
        }
    }
}
