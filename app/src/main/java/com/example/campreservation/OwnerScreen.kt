package com.example.campreservation

import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.campreservation.controller.OwnerController
import com.example.campreservation.model.CampsiteOwner
import com.example.campreservation.view.OwnerView

class OwnerScreen : Screen() {

    override val role = UserRole.OWNER

    private lateinit var model: CampsiteOwner
    private lateinit var controller: OwnerController
    private lateinit var ownerView: OwnerView
    private val listener: (CampsiteOwner) -> Unit = { ownerView.update(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ownerId = Settings.Secure.getString(
            requireContext().contentResolver, Settings.Secure.ANDROID_ID
        )
        model = CampsiteOwner(ownerId, "Owner", CampsiteRepository())
        controller = OwnerController(model)
        ownerView = OwnerView(controller)
        model.addListener(listener)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ownerView.Content() }
        }
    }

    override fun onDestroyView() {
        model.removeListener(listener)   // same function object that was added
        super.onDestroyView()
    }

    override fun show() { /* nothing extra: Compose draws itself */ }

    override fun refresh() = controller.refresh()
}