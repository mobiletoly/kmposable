package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactDetailsEvent
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactDetailsState

@Composable
fun ContactDetailsScreen(
    state: ContactDetailsState,
    onEvent: (ContactDetailsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            state.isLoading -> Text("Loading...", style = MaterialTheme.typography.bodyMedium)
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            else -> {
                Text(state.contact?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                Text(state.contact?.phone.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text(state.contact?.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onEvent(ContactDetailsEvent.EditClicked) }, enabled = state.contact != null) {
            Text("Edit")
        }
        Button(onClick = { onEvent(ContactDetailsEvent.DeleteClicked) }, enabled = state.contact != null) {
            Text("Delete")
        }
        OutlinedButton(onClick = { onEvent(ContactDetailsEvent.BackClicked) }) {
            Text("Back")
        }
    }
}
