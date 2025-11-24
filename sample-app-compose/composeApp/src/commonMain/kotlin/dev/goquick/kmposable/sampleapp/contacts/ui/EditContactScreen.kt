package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goquick.kmposable.sampleapp.contacts.EditContactEvent
import dev.goquick.kmposable.sampleapp.contacts.EditContactState

@Composable
fun EditContactScreen(
    state: EditContactState,
    onEvent: (EditContactEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(EditContactEvent.NameChanged(it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = { onEvent(EditContactEvent.PhoneChanged(it)) },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.email.orEmpty(),
            onValueChange = { onEvent(EditContactEvent.EmailChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { onEvent(EditContactEvent.SaveClicked) }, enabled = !state.isSaving) {
            Text("Save")
        }
        Button(onClick = { onEvent(EditContactEvent.CancelClicked) }) {
            Text("Cancel")
        }
    }
}
