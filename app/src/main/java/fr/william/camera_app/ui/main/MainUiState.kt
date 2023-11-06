package fr.william.camera_app.ui.main


sealed interface MainUiState {

    data object Loading : MainUiState
}