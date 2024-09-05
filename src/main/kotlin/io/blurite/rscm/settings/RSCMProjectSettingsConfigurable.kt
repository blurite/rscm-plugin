package io.blurite.rscm.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class RSCMProjectSettingsConfigurable(
    private val project: Project,
) : Configurable {
    private var directoryTextField: TextFieldWithBrowseButton? = null
    private var panel: JPanel? = null
    private val settings = RSCMProjectSettings.getInstance(project)

    override fun createComponent(): JComponent? {
        panel = JPanel(GridBagLayout())
        val gridBag = GridBag().setDefaultInsets(JBUI.insets(5)).setDefaultAnchor(GridBagConstraints.WEST)

        // Add a label for the directory path
        val directoryLabel = JLabel("Mappings Path:")
        panel?.add(directoryLabel, gridBag.nextLine().next().weightx(0.0))

        // Initialize the text field with a browse button
        directoryTextField = TextFieldWithBrowseButton()

        // Set preferred width for the text field to make it wider
        directoryTextField?.preferredSize = JBUI.size(400, 30)

        // Create a file chooser descriptor to allow only directory selection
        val fileChooserDescriptor: FileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fileChooserDescriptor.title = "Select Mappings Directory"

        // Add action listener to open the file chooser dialog when the button is clicked
        directoryTextField?.addBrowseFolderListener(
            "Select Directory",
            "Choose the RSCM mappings directory",
            project,
            fileChooserDescriptor,
        )

        // Add the text field with the browse button to the panel
        panel?.add(directoryTextField!!, gridBag.next().weightx(1.0).fillCellHorizontally())

        // Force the components to align at the top left by filling vertical and horizontal space
        gridBag.nextLine()
        gridBag.weighty(1.0) // Push components to the top by giving remaining vertical space
        panel?.add(Box.createVerticalGlue(), gridBag)

        return panel
    }

    override fun isModified(): Boolean = directoryTextField?.text != settings.mappingsPath

    override fun apply() {
        settings.mappingsPath = directoryTextField?.text.orEmpty()
    }

    override fun reset() {
        directoryTextField?.text = settings.mappingsPath
    }

    override fun getDisplayName(): String = "RSCM Settings"
}
