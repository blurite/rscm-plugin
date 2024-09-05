package io.blurite.rscm.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "RSCMProjectSettings",
    storages = [Storage("rscmProjectSettings.xml")],
)
class RSCMProjectSettings(
    private val project: Project,
) : PersistentStateComponent<RSCMProjectSettingsState> {
    private var state = RSCMProjectSettingsState()

    override fun getState(): RSCMProjectSettingsState = state

    override fun loadState(state: RSCMProjectSettingsState) {
        this.state = state
    }

    var mappingsPath: String
        get() = state.mappingsPath
        set(value) {
            state.mappingsPath = value
        }

    companion object {
        fun getInstance(project: Project): RSCMProjectSettings = project.getService(RSCMProjectSettings::class.java)
    }
}
